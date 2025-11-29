package model;

public final class Cinema
{
    private Cinema(){};                                              // Default constructor

    public static final int NUMBER_OF_ROW_SEATS = 10;                // Number of seats on each row

    public static final int ROWS_OF_ZONE_A = 10;                     // Number of Zone A rows
    public static final int ROWS_OF_ZONE_B = 20;                     // Number of Zone B rows

    public static double COST_OF_SEAT_A = 30;                        // Cost of Zone A seat
    public static double COST_OF_SEAT_B = 20;                        // Cost of Zone B seat

    public static final double PROBABILITY_OF_ZONE_A = 0.3;          // Chance of choosing Zone A
    public static final double PROBABILITY_OF_ZONE_B = 0.7;          // Chance of choosing Zone B

    public static final int NUMBER_OF_OPERATORS = 3;                 // Number of operators
    public static final int NUMBER_OF_CASHIERS = 2;                  // Number of cashiers

    public static final int MIN_SEATS_CHOSEN = 1;                    // Minimum number of chosen seats
    public static final int MAX_SEATS_CHOSEN = 5;                    // Maximum number of chosen seats

    public static final float MIN_SEARCH_TIME = 5;                   // Minimum searching time (seconds)
    public static final float MAX_SEARCH_TIME = 13;                  // Maximum searching time (seconds)

    public static final float MIN_PAYMENT_TIME = 4;                  // Minimum payment time (seconds)
    public static final float MAX_PAYMENT_TIME = 8;                  // Maximum payment time (seconds)

    public static final float MIN_CREATION_TIME = 1;                 // Minimum customer creation time (seconds)
    public static final float MAX_CREATION_TIME = 5;                 // Maximum customer creation time (seconds)

    public static final float PAYMENT_SUCCESS_PROBABILITY = 0.9f;    // Successful payment percentage

    public enum Zone { A, B }                                        // Cinema Zone values

    public enum SeatStatus { AVAILABLE, TAKEN }                      // Availability of cinema seats

    public enum State { SUCCESS, FAIL }                              // Operation result
}
