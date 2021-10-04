package com.hypertrack.android.gui


import com.hypertrack.android.ui.screens.place_details.PlaceVisitsAdapter
import com.hypertrack.android.ui.screens.visits_management.tabs.history.TimeDistanceFormatter
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.formatters.DateTimeFormatterImpl
import com.hypertrack.android.utils.formatters.DatetimeFormatter
import com.hypertrack.android.utils.formatters.prettyFormatDate

import com.hypertrack.logistics.android.github.R
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class GeofenceDateFormattingTest {

    @Test
    fun `it should properly format enter and exit range`() {
        val formatter: DatetimeFormatter = object : DateTimeFormatterImpl(ZoneId.of("UTC")) {
            override fun formatTime(dt: ZonedDateTime): String {
                return dt.format(DateTimeFormatter.ISO_DATE_TIME).replace("[UTC]", "")
            }
        }
        val osUtilsProvider = mockk<OsUtilsProvider>() {
            every { stringFromResource(R.string.place_today) } returns "Today"
            every { stringFromResource(R.string.place_yesterday) } returns "Yesterday"
        }
        val adapter = PlaceVisitsAdapter(osUtilsProvider, formatter, mockk(), mockk()) {}
        val today = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("UTC"))
            .withHour(13).withMinute(1)
        val yesterday = today.minusDays(1)
        val weekAgo = today.minusDays(7)
        val longTimeAgo = today.minusDays(14)

        test(adapter, today, today, osUtilsProvider, formatter) { res, dt1, dt2 ->
            println(res)
            val expected = "Today, ${formatter.formatTime(dt1)} — ${formatter.formatTime(dt2)}"
            println(expected)
            assertEquals(expected, res)
        }

        test(adapter, yesterday, yesterday, osUtilsProvider, formatter) { res, dt1, dt2 ->
            assertEquals(
                "Yesterday, ${formatter.formatTime(dt1)} — ${formatter.formatTime(dt2)}",
                res
            )
        }

        test(adapter, yesterday, today, osUtilsProvider, formatter) { res, dt1, dt2 ->
            assertEquals(
                "Yesterday, ${formatter.formatTime(dt1)} — Today, ${
                    formatter.formatTime(
                        dt2
                    )
                }", res
            )
        }

        test(adapter, weekAgo, yesterday, osUtilsProvider, formatter) { res, dt1, dt2 ->
            assertEquals(
                "${weekAgo.prettyFormatDate()}, ${formatter.formatTime(dt1)} — Yesterday, ${
                    formatter.formatTime(
                        dt2
                    )
                }", res
            )
        }

        test(adapter, weekAgo, weekAgo, osUtilsProvider, formatter) { res, dt1, dt2 ->
            assertEquals(
                "${weekAgo.prettyFormatDate()}, ${formatter.formatTime(dt1)} — ${
                    formatter.formatTime(
                        dt2
                    )
                }", res
            )
        }

        test(adapter, longTimeAgo, weekAgo, osUtilsProvider, formatter) { res, dt1, dt2 ->
            assertEquals(
                "${longTimeAgo.prettyFormatDate()}, ${formatter.formatTime(dt1)} — ${weekAgo.prettyFormatDate()}, ${
                    formatter.formatTime(
                        dt2
                    )
                }", res
            )
        }

    }

    fun test(
        adapter: PlaceVisitsAdapter,
        baseDt1: ZonedDateTime,
        baseDt2: ZonedDateTime,
        osUtilsProvider: OsUtilsProvider,
        datetimeFormatter: DatetimeFormatter,
        checks: (res: String, dt1: ZonedDateTime, dt2: ZonedDateTime) -> Unit
    ) {
        val dt1 = baseDt1
        val dt2 = baseDt2.withMinute(30).withSecond(1)

        PlaceVisitsAdapter.formatDate(
            dt1, dt2,
            osUtilsProvider,
            TimeDistanceFormatter(datetimeFormatter, mockk(relaxed = true)),
        ).let {
            checks.invoke(
                it,
                dt1,
                dt2
            )
        }
    }

}