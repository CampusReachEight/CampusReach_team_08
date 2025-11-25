package com.android.sample.model.request

import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Test

class RequestViewStatusTest {

  private fun makeRequest(
      status: RequestStatus = RequestStatus.OPEN,
      startOffsetMs: Long,
      endOffsetMs: Long
  ): Request {
    val now = Date().time
    return Request(
        requestId = "r1",
        title = "Test",
        description = "desc",
        requestType = listOf(RequestType.OTHER),
        location = com.android.sample.model.map.Location(0.0, 0.0, "test"),
        locationName = "loc",
        status = status,
        startTimeStamp = Date(now + startOffsetMs),
        expirationTime = Date(now + endOffsetMs),
        people = listOf(),
        tags = listOf(),
        creatorId = "user")
  }

  @Test
  fun statusIsOPENBeforeStart() {
    val req = makeRequest(startOffsetMs = 10_000, endOffsetMs = 20_000)
    assertEquals(RequestStatus.OPEN, req.viewStatus)
  }

  @Test
  fun statusIsIN_PROGRESSBetweenStartAndExpiration() {
    val req = makeRequest(startOffsetMs = -10_000, endOffsetMs = 10_000)
    assertEquals(RequestStatus.IN_PROGRESS, req.viewStatus)
  }

  @Test
  fun statusIsCOMPLETEDAfterExpiration() {
    val req = makeRequest(startOffsetMs = -20_000, endOffsetMs = -10_000)
    assertEquals(RequestStatus.COMPLETED, req.viewStatus)
  }

  @Test
  fun statusCANCELLEDRemainsCANCELLED() {
    val req =
        makeRequest(status = RequestStatus.CANCELLED, startOffsetMs = -10000, endOffsetMs = 10000)
    assertEquals(RequestStatus.CANCELLED, req.viewStatus)
  }

  @Test
  fun statusARCHIVEDRemainsARCHIVED() {
    val req =
        makeRequest(status = RequestStatus.ARCHIVED, startOffsetMs = -10000, endOffsetMs = 10000)
    assertEquals(RequestStatus.ARCHIVED, req.viewStatus)
  }

  @Test
  fun statusAtExactStartTime_isIN_PROGRESS() {
    val req = makeRequest(startOffsetMs = 0, endOffsetMs = 10_000)
    assertEquals(RequestStatus.IN_PROGRESS, req.viewStatus)
  }

  @Test
  fun statusAtExactExpirationTime_isCOMPLETED() {
    val req = makeRequest(startOffsetMs = -10_000, endOffsetMs = 0)
    assertEquals(RequestStatus.COMPLETED, req.viewStatus)
  }

  @Test
  fun startAfterExpiration_stillCompletes() {
    // endOffset is BEFORE startOffset → now is after expiration
    val req = makeRequest(startOffsetMs = 10_000, endOffsetMs = -10_000)
    assertEquals(RequestStatus.COMPLETED, req.viewStatus)
  }

  @Test
  fun startAndExpirationEqualNow_isCOMPLETED() {
    // start == expiration == now
    val req = makeRequest(startOffsetMs = 0, endOffsetMs = 0)
    assertEquals(RequestStatus.COMPLETED, req.viewStatus)
  }
}
