package model;
import model.Cinema;

public class CustomerRequest
{
    private long id;                   // Unique identifier
    private int transaction_number;    // Transaction number
    private int[] seats;               // Array of (maximum 5) seats the customer requests
    private int requested_seats;       // Zone the customer requested
    private Cinema.Zone requestedZone; // Number of requested seats
    private double cost;               // Total price the customer pays
    private Cinema.State state;        // State to represent the case the request is accepted or rejected
    private float waited;              // How long the customer is waiting (in seconds)

    // Default constructor
    public CustomerRequest()
    {
        this.id = -1;
        this.transaction_number = -1;
        this.seats = new int[Cinema.MAX_SEATS_CHOSEN];
        this.requested_seats = 0;
        this.requestedZone = Cinema.Zone.A;
        this.cost = 0.0;
        this.state = Cinema.State.FAIL;
        this.waited = 0.0f;
    }

    // Custom constructor
    public CustomerRequest(long id, int transaction_number, int[] seats, int requested_seats, Cinema.Zone requestedZone,
                    double cost, Cinema.State state, float waited)
    {
        this.id = id;
        this.transaction_number = transaction_number;
        this.seats = seats;
        this.requested_seats = requested_seats;
        this.requestedZone = requestedZone;
        this.cost = cost;
        this.state = state;
        this.waited = waited;
    }

    //-----------------------------------------SETTERS AND GETTERS------------------------------------------------------
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getTransaction_number() { return transaction_number; }
    public void setTransaction_number(int transaction_number) { this.transaction_number = transaction_number; }

    public int[] getSeats() { return seats; }
    public void setSeats(int[] seats) { this.seats = seats; }

    public int getRequested_seats() { return requested_seats; }
    public void setRequested_seats(int requested_seats) { this.requested_seats = requested_seats; }

    public Cinema.Zone getRequestedZone() { return requestedZone; }
    public void setRequestedZone(Cinema.Zone requestedZone) { this.requestedZone = requestedZone; }

    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }

    public Cinema.State getState() { return state; }
    public void setState(Cinema.State state) { this.state = state; }

    public float getWaited() { return waited; }
    public void setWaited(float waited) { this.waited = waited; }
    //-----------------------------------------------------------------------------------------------------------------

}
