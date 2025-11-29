package syncControl;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import model.Cinema;
import model.CustomerRequest;


public class SyncControl implements ISyncControl
{
    // SLF4J Logger
    private static final Logger logger = LoggerFactory.getLogger(SyncControl.class);

    // RNG
    private final Random syncRandom;
    private long syncSeed = System.currentTimeMillis();
    private final Object rngLock = new Object();

    // Operators
    private final ReentrantLock operatorLock = new ReentrantLock();
    private final Condition operatorCondition = operatorLock.newCondition();
    private int availableOperators = Cinema.NUMBER_OF_OPERATORS;
    private final AtomicInteger transactionNumber = new AtomicInteger(0);

    // Cashiers
    private final ReentrantLock cashierLock = new ReentrantLock();
    private final Condition cashierCondition = cashierLock.newCondition();
    private int availableCashiers = Cinema.NUMBER_OF_CASHIERS;

    // Seats
    private final ReentrantLock seatsLock = new ReentrantLock();
    private final int[] cinemaSeats;
    private int availableSeats;

    // Shared values
    private final AtomicLong totalRevenue = new AtomicLong(0);
    private final AtomicInteger successfulTransactions = new AtomicInteger(0);
    private final AtomicInteger failedDueToUnavailableSeats = new AtomicInteger(0);
    private final AtomicInteger failedDueToPayment = new AtomicInteger(0);
    private final AtomicLong sumOfTransactionsTime =  new AtomicLong(0); // ms
    private final AtomicLong sumOfWaitingTime =  new AtomicLong(0); // ms

    // Constructor
    public SyncControl()
    {
        int totalSeats = (Cinema.ROWS_OF_ZONE_A + Cinema.ROWS_OF_ZONE_B) * Cinema.NUMBER_OF_ROW_SEATS;
        this.cinemaSeats = new int[totalSeats];
        this.availableSeats = totalSeats;
        this.syncRandom = new Random(syncSeed);
    }

    // Custom Constructor to add seed
    public SyncControl(long seed)
    {
        this(); // Call default constructor
        this.syncSeed = seed;
        this.syncRandom.setSeed(seed);
    }

    // Generate random integer
    public int nextIntValue(int bound)
    {
        if (bound <= 0)
        {
            throw new IllegalArgumentException("nextIntValue(bound) result must be > 0");
        }

        synchronized (rngLock)
        {
            return syncRandom.nextInt(bound);
        }
    }


    // Generate random double
    public double nextDoubleValue()
    {
        synchronized (rngLock)
        {
            return syncRandom.nextDouble();
        }
    }


    //----------------------------------INTERFACE IMPLEMENTATIONS-------------------------------------------------------
    @Override
    public void acquireOperator(CustomerRequest request) throws InterruptedException
    {
        MDC.put("customerId", String.valueOf(request.getId()));
        operatorLock.lock(); // Lock cinema's operator
        try
        {
            long waitStart = System.currentTimeMillis(); // Start timer for phone call

            while(availableOperators <= 0) // Wait until there are available operators
            {
                operatorCondition.await();
            }
            availableOperators--; // The operator answers the customer's call
            request.setTransaction_number(transactionNumber.getAndIncrement()); // New transaction

            long waitEnd = System.currentTimeMillis(); // Stop timer
            long waited = waitEnd - waitStart; // How long the customer waited for phone call

            request.setWaited(request.getWaited() + (float)(waited / 1000.0f));  // Milliseconds to seconds
            sumOfWaitingTime.addAndGet(waited); // Update sum
            logger.info("Customer <{}> Acquired operator, waited {} ms", request.getId() ,waited);
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while waiting for an operator: {}", e.getMessage());
            MDC.clear();
            throw e;
        }
        finally
        {
            operatorLock.unlock(); // Unlock cinema's operator
            MDC.clear();
        }
    }

    @Override
    public void releaseOperator()
    {
        operatorLock.lock(); // Lock occupied operator

        try
        {
            availableOperators++;
            operatorCondition.signalAll(); // Notify that operator is released
            logger.info("Released operator");
        }
        finally
        {
            operatorLock.unlock(); // Unlock occupied operator
        }
    }


    @Override
    public void acquireCashier() throws InterruptedException
    {
        cashierLock.lock(); // Lock cinema's cashier

        try
        {
            long waitStart = System.currentTimeMillis(); // Start timer
            while(availableCashiers <= 0)
            {
                cashierCondition.await(); // Wait until a cashier is available for payment
            }
            availableCashiers--;
            long waitEnd = System.currentTimeMillis(); // Stop timer
            long waited = waitEnd - waitStart; // How long the customer waited for a cashier

            sumOfWaitingTime.addAndGet(waited);
            logger.info("Acquired cashier, waited {} ms", waited);
        }
        finally
        {
            cashierLock.unlock(); // Unlock cinema's cashier
        }
    }

    @Override
    public void releaseCashier()
    {
        cashierLock.lock(); // Lock occupied cashier

        try
        {
            availableCashiers++;
            cashierCondition.signalAll(); // Notify the cashier is released
            logger.info("Released cashier");
        }
        finally
        {
            cashierLock.unlock(); // Unlock occupied cashier
        }
    }

    @Override
    public Cinema.State reserveSeats(CustomerRequest request)
    {
        int range = (int)Cinema.MAX_SEARCH_TIME - (int)Cinema.MIN_SEARCH_TIME + 1; // RNG time range
        int searchTime = nextIntValue(range) + (int)Cinema.MIN_SEARCH_TIME;

        long sleepTime = searchTime * 1000L; // Milliseconds to seconds

        try
        {
            Thread.sleep(sleepTime); // Sleep a few seconds
        }
        catch(InterruptedException e)
        {
            logger.error("Error occurred while reserving seats: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return Cinema.State.FAIL;
        }

        int zoneStart, zoneEnd; // Indexes of seats' array

        if(request.getRequestedZone() == Cinema.Zone.A) // Zone A
        {
            zoneStart = 0;
            zoneEnd = Cinema.ROWS_OF_ZONE_A * Cinema.NUMBER_OF_ROW_SEATS;
        }
        else // Zone B
        {
            zoneStart = Cinema.ROWS_OF_ZONE_A * Cinema.NUMBER_OF_ROW_SEATS;
            zoneEnd = (Cinema.ROWS_OF_ZONE_A + Cinema.ROWS_OF_ZONE_B) * Cinema.NUMBER_OF_ROW_SEATS;
        }

        int seatsAsked = request.getRequested_seats();
        int seatsAskedCount = 0;

        for (int seat = zoneStart; seat <= zoneEnd - seatsAsked; seat++)
        {
            int seatsLeftInRow = Cinema.NUMBER_OF_ROW_SEATS - (seat % Cinema.NUMBER_OF_ROW_SEATS);
            if (seatsLeftInRow < seatsAsked) continue;

            if (tryReserveAt(seat, seatsAsked, request))
            {
                seatsAskedCount = seatsAsked;
                break;
            }
        }

        // Rollback if not enough reserved
        if (seatsAskedCount < seatsAsked)
        {
            rollbackSeatReservations(request, seatsAskedCount);
            return Cinema.State.FAIL;
        }

        return Cinema.State.SUCCESS;
    }

    @Override
    public Cinema.Zone askZone(CustomerRequest request)
    {
        double zoneProbability = nextDoubleValue();
        return (zoneProbability < Cinema.PROBABILITY_OF_ZONE_A) ? Cinema.Zone.A : Cinema.Zone.B;
    }

    @Override
    public Cinema.State findSeats(CustomerRequest request)
    {
        // How many seats are asked
        int requestedSeats = nextIntValue(Cinema.MAX_SEATS_CHOSEN) + Cinema.MIN_SEATS_CHOSEN;
        request.setRequested_seats(requestedSeats);

        // What zone is asked
        request.setRequestedZone(askZone(request));

        MDC.put("customerId", String.valueOf(request.getId()));
        try
        {
            logger.info("Customer <{}> Number of desired seats: {}. Proceed to checking...", request.getId(), requestedSeats);
            Cinema.State result = reserveSeats(request);

            if (result == Cinema.State.SUCCESS)
            {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < requestedSeats; i++) { sb.append(request.getSeats()[i] + 1).append(" "); }
                logger.info("Customer <{}> Seats found: {}", request.getId(), sb.toString().trim());
                logger.info("Customer <{}> Found your seats. Proceed to ticket payment...", request.getId());
                return Cinema.State.SUCCESS;
            }
            else
            {
                logger.info("Customer <{}> Unfortunately, there are no seats available...", request.getId());
                failedDueToUnavailableSeats.incrementAndGet();
                request.setState(Cinema.State.FAIL);
                return Cinema.State.FAIL;
            }

        }
        finally
        {
            MDC.clear();
        }
    }

    @Override
    public void changeAvailabilityStatus(CustomerRequest request, Cinema.SeatStatus newStatus)
    {
        seatsLock.lock();
        try {
            int value = (newStatus == Cinema.SeatStatus.AVAILABLE) ? 0 : (int) request.getId();
            for (int i = 0; i < request.getRequested_seats(); i++)
            {
                int seatIndex = request.getSeats()[i];

                if (seatIndex >= 0 && seatIndex < cinemaSeats.length)
                {
                    cinemaSeats[seatIndex] = value;
                }
            }
            if (newStatus == Cinema.SeatStatus.AVAILABLE)
            {
                availableSeats += request.getRequested_seats();
            }
        }
        finally
        {
            seatsLock.unlock();
        }
    }

    @Override
    public boolean tryReserveAt(int startIndex, int seatsNeeded, CustomerRequest request)
    {
        if (startIndex < 0 || startIndex + seatsNeeded > cinemaSeats.length) return false;

        seatsLock.lock();
        try
        {
            for (int j = startIndex; j < startIndex + seatsNeeded; j++)
            {
                if (cinemaSeats[j] != 0) return false;
            }
            for (int j = startIndex, k = 0; k < seatsNeeded; j++, k++)
            {
                cinemaSeats[j] = -1;
                request.getSeats()[k] = j;
            }
            availableSeats -= seatsNeeded;
            return true;
        }
        finally
        {
            seatsLock.unlock();
        }
    }

    @Override
    public void rollbackSeatReservations(CustomerRequest request, int reservedSeatsCount)
    {
        seatsLock.lock();
        try {
            int restored = 0;
            for (int idx = 0; idx < reservedSeatsCount; idx++) {
                int s = request.getSeats()[idx];
                if (s >= 0 && s < cinemaSeats.length && cinemaSeats[s] == -1)
                {
                    cinemaSeats[s] = 0;
                    restored++;
                }
            }
            if (restored > 0) availableSeats += restored;
        }
        finally
        {
            seatsLock.unlock();
        }
    }

    @Override
    public void processPayment(CustomerRequest request)
    {
        MDC.put("customerId", String.valueOf(request.getId()));
        int range = (int) Cinema.MAX_PAYMENT_TIME - (int) Cinema.MIN_PAYMENT_TIME + 1;
        long paymentTime = nextIntValue(range) + (int) Cinema.MIN_PAYMENT_TIME * 1000L;

        try
        {
            Thread.sleep(paymentTime);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            logger.error("Interrupted during payment processing");
            return;
        }

        // Calculate cost
        double cost = (request.getRequestedZone() == Cinema.Zone.A)
                ? request.getRequested_seats() * Cinema.COST_OF_SEAT_A
                : request.getRequested_seats() * Cinema.COST_OF_SEAT_B;
        request.setCost(cost);

        Cinema.State paymentState = generatePaymentProbability((int) cost);
        if (paymentState == Cinema.State.SUCCESS)
        {
            changeAvailabilityStatus(request, Cinema.SeatStatus.TAKEN);
            successfulTransactions.incrementAndGet();
            totalRevenue.addAndGet((long) cost);
            request.setState(Cinema.State.SUCCESS);
            logger.info("Customer <{}> Payment successful! Seats confirmed.", request.getId());
        }
        else
        {
            changeAvailabilityStatus(request, Cinema.SeatStatus.AVAILABLE);
            failedDueToPayment.incrementAndGet();
            request.setState(Cinema.State.FAIL);
            logger.info("Customer <{}> Payment failed. Booking canceled.", request.getId());
        }
    }

    @Override
    public Cinema.State generateProbability(float percentage)
    {
        double p = nextDoubleValue();
        return (p < percentage) ? Cinema.State.SUCCESS : Cinema.State.FAIL;
    }

    @Override
    public Cinema.State generatePaymentProbability(int amount)
    {
        return generateProbability(Cinema.PAYMENT_SUCCESS_PROBABILITY);
    }
    //------------------------------------------------------------------------------------------------------------------

    //-----------------------------------------SETTERS AND GETTERS------------------------------------------------------
    public long getSyncSeed() {  return syncSeed; }
    public void setSyncSeed(long syncSeed) { this.syncSeed = syncSeed; }

    public ReentrantLock getOperatorLock() { return operatorLock; }
    public Condition getOperatorCondition() { return operatorCondition; }

    public int getAvailableOperators() { return availableOperators; }
    public void setAvailableOperators(int availableOperators) { this.availableOperators = availableOperators; }

    public AtomicInteger getTransactionNumber() { return transactionNumber; }

    public ReentrantLock getCashierLock() { return cashierLock; }
    public Condition getCashierCondition() { return cashierCondition; }

    public int getAvailableCashiers() { return availableCashiers; }
    public void setAvailableCashiers(int availableCashiers) { this.availableCashiers = availableCashiers; }

    public ReentrantLock getSeatsLock() { return seatsLock; }
    public int[] getCinemaSeats() { return cinemaSeats; }

    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }

    public AtomicLong getTotalRevenue() { return totalRevenue; }

    public AtomicInteger getSuccessfulTransactions() { return successfulTransactions; }
    public AtomicInteger getFailedDueToUnavailableSeats() { return failedDueToUnavailableSeats; }
    public AtomicInteger getFailedDueToPayment() { return failedDueToPayment; }

    public AtomicLong getSumOfTransactionsTime() { return sumOfTransactionsTime; }
    public AtomicLong getSumOfWaitingTime() { return sumOfWaitingTime; }
    //------------------------------------------------------------------------------------------------------------------
}
