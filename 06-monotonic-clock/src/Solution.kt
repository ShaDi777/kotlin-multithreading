/**
 * В теле класса решения разрешено использовать только переменные делегированные в класс RegularInt.
 * Нельзя volatile, нельзя другие типы, нельзя блокировки, нельзя лазить в глобальные переменные.
 *
 * @author Shalanov Dmitriy
 */
class Solution : MonotonicClock {
    private var c1d1 by RegularInt(0)
    private var c1d2 by RegularInt(0)
    private var c1d3 by RegularInt(0)

    private var c2d1 by RegularInt(0)
    private var c2d2 by RegularInt(0)
    private var c2d3 by RegularInt(0)

    override fun write(time: Time) {
        // c2 left-to-right new time
        c2d1 = time.d1
        c2d2 = time.d2
        c2d3 = time.d3

        // c1 right-to-left c2
        c1d3 = c2d3
        c1d2 = c2d2
        c1d1 = c2d1
    }

    override fun read(): Time {
        // left-to-right c1
        val r1 = Time(c1d1, c1d2, c1d3)

        // right-to-left c2
        val d3 = c2d3
        val d2 = c2d2
        val d1 = c2d1
        val r2 = Time(d1, d2, d3)

        return if (r1 == r2) {
            r2
        } else {
            Time(
                r2.d1,
                if (r1.d1 == r2.d1) r2.d2 else 0,
                if (r1.d1 == r2.d1 && r1.d2 == r2.d2) r2.d3 else 0,
            )
        }
    }
}