package com.android.sample.ui.request

import com.android.sample.ui.request.edit.DateTimePickerState
import java.util.Date
import org.junit.Test

class DateTimePickerStateTest {

  @Test
  fun handleStartDateSelected_updatesStateCorrectly() {
    var receivedDate: Date? = null
    val state =
        DateTimePickerState(
            onStartDateTimeChange = { receivedDate = it }, onExpirationDateTimeChange = {})
    val testDate = Date(1735689600000L)

    state.handleStartDateSelected(testDate)

    assert(state.tempSelectedDate == testDate)
    assert(!state.showStartDatePicker)
    assert(state.showStartTimePicker)
  }

  @Test
  fun handleStartTimeSelected_callsActionAndClearsState() {
    var receivedDate: Date? = null
    val state =
        DateTimePickerState(
            onStartDateTimeChange = { receivedDate = it }, onExpirationDateTimeChange = {})
    state.tempSelectedDate = Date(1735689600000L)

    state.handleStartTimeSelected(14, 30)

    assert(receivedDate != null)
    assert(!state.showStartTimePicker)
    assert(state.tempSelectedDate == null)
  }

  @Test
  fun handleStartTimeDismiss_clearsState() {
    val state = DateTimePickerState(onStartDateTimeChange = {}, onExpirationDateTimeChange = {})
    state.showStartTimePicker = true
    state.tempSelectedDate = Date()

    state.handleStartTimeDismiss()

    assert(!state.showStartTimePicker)
    assert(state.tempSelectedDate == null)
  }

  @Test
  fun handleExpirationDateSelected_updatesStateCorrectly() {
    val state = DateTimePickerState(onStartDateTimeChange = {}, onExpirationDateTimeChange = {})
    val testDate = Date(1735689600000L)

    state.handleExpirationDateSelected(testDate)

    assert(state.tempSelectedDate == testDate)
    assert(!state.showExpirationDatePicker)
    assert(state.showExpirationTimePicker)
  }

  @Test
  fun handleExpirationTimeSelected_callsActionAndClearsState() {
    var receivedDate: Date? = null
    val state =
        DateTimePickerState(
            onStartDateTimeChange = {}, onExpirationDateTimeChange = { receivedDate = it })
    state.tempSelectedDate = Date(1735689600000L)

    state.handleExpirationTimeSelected(23, 59)

    assert(receivedDate != null)
    assert(!state.showExpirationTimePicker)
    assert(state.tempSelectedDate == null)
  }
}
