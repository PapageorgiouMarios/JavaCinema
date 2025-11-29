import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import model.*;
import syncControl.*;

public class Main
{
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    static void main(String[] args) throws InterruptedException
    {
        // Check program's arguments
        checkArguments(args);

        // Set parameters after successful checks
        int numberOfCustomers = Integer.parseInt(args[0]);
        long globalSeed = Math.abs(Long.parseLong(args[1]));
        SyncControl sync = new SyncControl(globalSeed);

        // How many seats exist
        logger.info("Cinema's total seats: {}", sync.getCinemaSeats().length);

        // Customers
        int[] customerIds = new int[numberOfCustomers];

        // Set all ids starting from id = 1
        for(int i = 0; i < numberOfCustomers; i++) { customerIds[i] = i + 1; }

        // Customer Threads
        List<Thread> customerThreads = new ArrayList<>();

        for(int customer = 0; customer < numberOfCustomers; customer++)
        {
            int customerId = customerIds[customer]; // Get customer

            Runnable r = () ->
            {
                MDC.put("customerId", String.valueOf(customerId));
                long timeStart = System.currentTimeMillis();

                try
                {
                    CustomerRequest request = new CustomerRequest();
                    request.setId(customerId);
                    MDC.put("customerId", String.valueOf(customerId));
                    logger.info("Customer <{}> is calling", customerId);
                    sync.acquireOperator(request);

                    if(sync.getAvailableSeats() == 0)
                    {
                        logger.info("Customer <{}> No seats available for booking", customerId);
                        request.setState(Cinema.State.FAIL);
                    }
                    else
                    {
                        Cinema.State found = sync.findSeats(request);
                        sync.releaseOperator();

                        if(found == Cinema.State.SUCCESS)
                        {
                            sync.acquireCashier();
                            sync.processPayment(request);
                            sync.releaseCashier();
                        }
                    }
                }
                catch(InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    logger.error("Customer <{}> thread interrupted: {}", customerId, e.getMessage());
                }

                long timeEnd = System.currentTimeMillis();
                long duration = timeEnd - timeStart;
                sync.getSumOfTransactionsTime().addAndGet(duration);
                MDC.clear();
            };

            if(customer == 0)
            {
                Thread t = new Thread(r);
                customerThreads.add(t);
                t.start();
            }
            else
            {
                int range = (int)(Cinema.MAX_CREATION_TIME - Cinema.MIN_CREATION_TIME + 1);
                int delay = sync.nextIntValue(range) + (int)Cinema.MIN_CREATION_TIME;

                Thread.sleep(delay * 1000L);

                Thread t = new Thread(r);
                customerThreads.add(t);
                t.start();
            }
        }

        for (Thread t : customerThreads) { t.join(); }
        logger.info("Plan of seats:");

        int totalSeats = Cinema.NUMBER_OF_ROW_SEATS * (Cinema.ROWS_OF_ZONE_A + Cinema.ROWS_OF_ZONE_B);
        int[] seats = sync.getCinemaSeats();

        for (int j = 0; j < totalSeats; j++)
        {
            String zone = (j < Cinema.NUMBER_OF_ROW_SEATS * Cinema.ROWS_OF_ZONE_A) ? "Zone A" : "Zone B";
            int row = (j / Cinema.NUMBER_OF_ROW_SEATS) + 1;
            if (zone.equals("Zone B")) row -= Cinema.ROWS_OF_ZONE_A;
            String seatStatus = (seats[j] != 0) ? "Customer " + seats[j] : "Empty";
            logger.info("{} / Row {} / Seat {} / {}", zone, row, j + 1, seatStatus);
        }

        long successful = sync.getSuccessfulTransactions().get();
        long failedSeats = sync.getFailedDueToUnavailableSeats().get();
        long failedPayment = sync.getFailedDueToPayment().get();
        long totalTx = successful + failedSeats + failedPayment;

        logger.info("Total revenue: {}$", sync.getTotalRevenue().get());
        logger.info("Total transactions: {}", sync.getTransactionNumber().get());
        logger.info("Total successful transactions: {}", successful);
        logger.info("Total failed transactions due to unavailable seats: {}", failedSeats);
        logger.info("Total failed transactions due to failed payment: {}", failedPayment);

        double avgWaiting = sync.getSumOfWaitingTime().get() / 1000.0 / totalTx;
        double avgTransaction = sync.getSumOfTransactionsTime().get() / 1000.0 / totalTx;

        logger.info("Average waiting time: {} seconds", String.format("%.2f", avgWaiting));
        logger.info("Average transaction time: {} seconds", String.format("%.2f", avgTransaction));

        double p1 = (successful * 100.0) / totalTx;
        double p2 = (failedSeats * 100.0) / totalTx;
        double p3 = (failedPayment * 100.0) / totalTx;

        logger.info("Percentage of successful transactions: {}%", (int) p1);
        logger.info("Percentage of failed transactions due to unavailable seats: {}%", (int) p2);
        logger.info("Percentage of failed transactions due to unsuccessful payment: {}%", (int) p3);
    }

    private static void checkArguments(String[] args)
    {
        if(args == null)
        {
            throw new IllegalArgumentException("Error: No arguments provided");
        }
        else if(args.length != 2)
        {
            throw new IllegalArgumentException("Error: Invalid arguments! Valid: 1) Number of customers 2) RNG Seed");
        }

        int numberOfCustomers;
        long globalSeed;

        try
        {
            numberOfCustomers = Integer.parseInt(args[0]);
            globalSeed = Math.abs(Long.parseLong(args[1]));

            if(numberOfCustomers <= 0)
            {
                throw new IllegalArgumentException("Number of customers must be positive");
            }
            logger.info("Number of customers: {}", numberOfCustomers);
            logger.info("Seed: {}", globalSeed);
        }
        catch(NumberFormatException e)
        {
            throw new IllegalArgumentException("Arguments must be: integer first argument, long second argument");
        }
    }
}
