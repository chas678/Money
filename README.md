#Money class

An implementation of Martin Fowlers Money/Value/Quantity patterns found in PofEAA book.
##See docs folder for more info.

##Money class, Represents a monetary value.

For a full description see the book Patterns of Enterprise Application Architecture (Martin Fowler) page 488 or
http://www.martinfowler.com/eaaCatalog/money.html

A large proportion of the computers in this world manipulate money, so it's always puzzled me that money isn't
actually a first class data type in any mainstream programming language. The lack of a type causes problems, the most
obvious surrounding currencies. If all your calculations are done in a single currency, this isn't a huge problem,
but once you involve multiple currencies you want to avoid adding your dollars to your yen without taking the
currency differences into account. The more subtle problem is with rounding. Monetary calculations are often rounded
to the smallest currency unit. When you do this it's easy to lose pennies (or your local equivalent) because of
rounding errors.

The good thing about object-oriented programming is that you can fix these problems by creating a Money class that
handles them. Of course, it's still surprising that none of the mainstream base class libraries actually do this. -
Martin Fowler

ISO4217 codes for currency http://www.xe.com/iso4217.htm

###Example Usage:

    aMoney = Money.dollars(12.98);
    bMoney = Money.dollars(-11.98);
    Money expected = Money.dollars(1.00);
    Money result = aMoney.add(bMoney);
    assert expected.equals(result) : "expected a dollar result after operation";


###Notes on Representing money :

use BigDecimal, int, or long (BigDecimal is the recommended default) the int and long forms represent pennies (or the
equivalent, of course) BigDecimal is a little more inconvenient to use, but has built-in rounding modes double or
float are not recommended, since they always carry small rounding differences the Currency class encapsulates
standard identifiers for the world's currencies

###Number of digits :

<=9 : use int, long , or BigDecimal
<=18 : use long or BigDecimal
>18 : use BigDecimal

###Reminders for BigDecimal :

the recommended constructor is BigDecimal(String), not BigDecimal(double) - see javadoc BigDecimal objects are
immutable - operations always return new objects, and never modify the state of existing objects the ROUND_HALF_EVEN
style of rounding introduces the least bias. It is also called bankers' rounding, or round-to-even.


