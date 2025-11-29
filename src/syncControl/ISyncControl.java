package syncControl;
import model.Cinema;
import model.CustomerRequest;

interface ISyncControl
{
    // Operators' Locks
    void acquireOperator(CustomerRequest request) throws InterruptedException;
    void releaseOperator();

    // Cashiers' Locks
    void acquireCashier() throws InterruptedException;
    void releaseCashier();

    // Cinema Seats
    Cinema.State reserveSeats(CustomerRequest request);
    Cinema.Zone askZone(CustomerRequest request);
    Cinema.State findSeats(CustomerRequest request);
    void changeAvailabilityStatus(CustomerRequest request, Cinema.SeatStatus newStatus);
    boolean tryReserveAt(int startIndex, int seatsNeeded, CustomerRequest request);
    void rollbackSeatReservations(CustomerRequest request, int reservedSeatsCount);

    // Payment process after reservation
    void processPayment(CustomerRequest request);

    // RNG (Random Number Generator)
    Cinema.State generateProbability(float percentage);
    Cinema.State generatePaymentProbability(int amount);
}
