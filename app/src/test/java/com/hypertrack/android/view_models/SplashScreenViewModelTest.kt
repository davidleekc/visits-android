package com.hypertrack.android.view_models

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hypertrack.android.api.MainCoroutineScopeRule
import com.hypertrack.android.observeAndAssertNull
import com.hypertrack.android.observeAndGetValue
import com.hypertrack.android.repository.AccountRepository
import com.hypertrack.android.repository.DriverRepository
import com.hypertrack.android.ui.screens.splash_screen.SplashScreenFragmentDirections
import com.hypertrack.android.ui.screens.splash_screen.SplashScreenViewModel
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.Injector
import com.hypertrack.logistics.android.github.R
import com.squareup.moshi.Types
import io.mockk.*
import junit.framework.TestCase.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test

class SplashScreenVieswModelTest {

    @ExperimentalCoroutinesApi
    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Test
    fun `handle empty deeplink`() {
        val vm = createVm()
        vm.handleDeeplink(mapOf(), mockk(relaxed = true))
        vm.destination.observeAndGetValue().payload.let {
            assertEquals(
                SplashScreenFragmentDirections.actionSplashScreenFragmentToSignInFragment(),
                it
            )
        }
    }

    @Test
    fun `handle empty deeplink (if logged in)`() {
        val driverRepository: DriverRepository = mockk(relaxed = true)
        val vm = createVm(driverRepository = driverRepository, loggedIn = true)
        vm.handleDeeplink(mapOf(), mockk(relaxed = true))
        vm.destination.observeAndGetValue().payload.let {
            assertEquals(
                SplashScreenFragmentDirections.actionGlobalVisitManagementFragment(),
                it
            )
        }
        verify(exactly = 0) {
            driverRepository.setUserData(allAny())
        }
    }

    @Test
    fun `handle invalid deeplink`() {
        val slot = slot<Exception>()
        val vm = createVm(slot)

        vm.handleDeeplink(
            mapOf(
                "a" to "b"
            ), mockk(relaxed = true)
        )

        assertEquals("publishableKey == null", slot.captured.message)

        vm.errorHandler.errorText.observeAndGetValue().let {
            assertEquals(R.string.splash_screen_invalid_link.toString(), it.value)
        }
        vm.destination.observeAndGetValue().payload.let {
            assertEquals(
                SplashScreenFragmentDirections.actionSplashScreenFragmentToSignInFragment(),
                it
            )
        }
    }

    @Test
    fun `handle invalid deeplink (if logged in)`() {
        val slot = slot<Exception>()
        val driverRepository: DriverRepository = mockk(relaxed = true)
        val vm = createVm(slot, driverRepository = driverRepository, loggedIn = true)

        vm.handleDeeplink(
            mapOf(
                "a" to "b"
            ), mockk(relaxed = true)
        )

        assertEquals("publishableKey == null", slot.captured.message)

        vm.errorHandler.errorText.observeAndGetValue().let {
            assertEquals(R.string.splash_screen_invalid_link.toString(), it.value)
        }
        vm.destination.observeAndGetValue().payload.let {
            assertEquals(
                SplashScreenFragmentDirections.actionGlobalVisitManagementFragment(),
                it
            )
        }

        verify(exactly = 0) {
            driverRepository.setUserData(allAny())
        }
    }

    @Test
    fun `handle old deeplink with just pk`() {
        val slot = slot<Exception>()
        val vm = createVm(slot)

        vm.handleDeeplink(
            mapOf(
                "publishable_key" to "key"
            ), mockk(relaxed = true)
        )

        assertEquals(
            "email == null && phoneNumber == null && driverId == null",
            slot.captured.message
        )

        vm.errorHandler.errorText.observeAndGetValue().let {
            assertEquals(R.string.splash_screen_invalid_link.toString(), it.value)
        }
        vm.destination.observeAndGetValue().payload.let {
            assertEquals(
                SplashScreenFragmentDirections.actionSplashScreenFragmentToSignInFragment(),
                it
            )
        }
    }

    @Test
    fun `handle old deeplink with just pk (if logged in)`() {
        val slot = slot<Exception>()
        val driverRepository: DriverRepository = mockk(relaxed = true)
        val vm = createVm(slot, driverRepository = driverRepository, loggedIn = true)

        vm.handleDeeplink(
            mapOf(
                "publishable_key" to "key"
            ), mockk(relaxed = true)
        )

        assertEquals(
            "email == null && phoneNumber == null && driverId == null",
            slot.captured.message
        )

        vm.errorHandler.errorText.observeAndGetValue().let {
            assertEquals(R.string.splash_screen_invalid_link.toString(), it.value)
        }
        vm.destination.observeAndGetValue().payload.let {
            assertEquals(
                SplashScreenFragmentDirections.actionGlobalVisitManagementFragment(),
                it
            )
        }

        verify(exactly = 0) {
            driverRepository.setUserData(allAny())
        }
    }

    @Test
    fun `old deeplink with driver_id`() {
        val driverRepository = mockk<DriverRepository>(relaxed = true)

        fun assertCheck(vm: SplashScreenViewModel, driverId: String) {
            vm.errorHandler.errorText.observeAndAssertNull()

            vm.destination.observeAndGetValue().payload.let {
                assertEquals(
                    SplashScreenFragmentDirections.actionGlobalVisitManagementFragment(),
                    it
                )
            }
        }

        createVm(driverRepository = driverRepository).let { vm ->
            vm.handleDeeplink(
                mapOf(
                    "publishable_key" to "key",
                    "driver_id" to "email@mail.com",
                ), mockk(relaxed = true)
            )

            assertCheck(vm, "email@mail.com")

            verify {
                driverRepository.setUserData(driverId = "email@mail.com")
            }
        }

        createVm(driverRepository = driverRepository).let { vm ->
            vm.handleDeeplink(
                mapOf(
                    "publishable_key" to "key",
                    "driver_id" to "Driver Id",
                ), mockk(relaxed = true)
            )

            assertCheck(vm, "Driver Id")

            verify {
                driverRepository.setUserData(driverId = "Driver Id")
            }
        }

    }

    @Test
    fun `old deeplink with driver_id (if logged in)`() {
        val driverRepository = mockk<DriverRepository>(relaxed = true)

        fun assertCheck(vm: SplashScreenViewModel, driverId: String) {
            vm.errorHandler.errorText.observeAndAssertNull()

            vm.destination.observeAndGetValue().payload.let {
                assertEquals(
                    SplashScreenFragmentDirections.actionGlobalVisitManagementFragment(),
                    it
                )
            }
        }

        val accountRepository = createAccountRepo(true)
        createVm(
            driverRepository = driverRepository,
            accountRepository = accountRepository
        ).let { vm ->
            vm.handleDeeplink(
                mapOf(
                    "publishable_key" to "key",
                    "driver_id" to "email@mail.com",
                ), mockk(relaxed = true)
            )

            assertCheck(vm, "email@mail.com")

            coVerify {
                driverRepository.setUserData(driverId = "email@mail.com")
                accountRepository.onKeyReceived("key")
            }
        }

        createVm(
            driverRepository = driverRepository,
            accountRepository = accountRepository
        ).let { vm ->
            vm.handleDeeplink(
                mapOf(
                    "publishable_key" to "key1",
                    "driver_id" to "Driver Id",
                ), mockk(relaxed = true)
            )

            assertCheck(vm, "Driver Id")

            coVerify {
                driverRepository.setUserData(driverId = "Driver Id")
                accountRepository.onKeyReceived("key1")
            }
        }

    }

    @Test
    fun `handle new deeplink`() {
        val driverRepository = mockk<DriverRepository>(relaxed = true)

        fun assertCheck(vm: SplashScreenViewModel) {
            vm.errorHandler.errorText.observeAndAssertNull()

            vm.destination.observeAndGetValue().payload.let {
                assertEquals(
                    SplashScreenFragmentDirections.actionGlobalVisitManagementFragment(),
                    it
                )
            }
        }

        createVm(driverRepository = driverRepository).let { vm ->
            vm.handleDeeplink(
                mapOf(
                    "publishable_key" to "key",
                    "email" to "email@mail.com",
                ), mockk(relaxed = true)
            )

            assertCheck(vm)

            verify {
                driverRepository.setUserData(email = "email@mail.com")
            }
        }

        createVm(driverRepository = driverRepository).let { vm ->
            vm.handleDeeplink(
                mapOf(
                    "publishable_key" to "key",
                    "phone_number" to "Phone",
                ), mockk(relaxed = true)
            )

            assertCheck(vm)

            verify {
                driverRepository.setUserData(phoneNumber = "Phone")
            }
        }
    }

    @Test
    fun `handle new deeplink (if logged in)`() {
        val driverRepository = mockk<DriverRepository>(relaxed = true)

        fun assertCheck(vm: SplashScreenViewModel) {
            vm.errorHandler.errorText.observeAndAssertNull()

            vm.destination.observeAndGetValue().payload.let {
                assertEquals(
                    SplashScreenFragmentDirections.actionGlobalVisitManagementFragment(),
                    it
                )
            }
        }

        val accountRepository = createAccountRepo(true)
        createVm(
            driverRepository = driverRepository,
            accountRepository = accountRepository
        ).let { vm ->
            vm.handleDeeplink(
                mapOf(
                    "publishable_key" to "key",
                    "email" to "email@mail.com",
                ), mockk(relaxed = true)
            )

            assertCheck(vm)

            coVerify {
                driverRepository.setUserData(email = "email@mail.com")
                accountRepository.onKeyReceived("key")
            }
        }

        createVm(
            driverRepository = driverRepository,
            accountRepository = accountRepository
        ).let { vm ->
            vm.handleDeeplink(
                mapOf(
                    "publishable_key" to "key1",
                    "phone_number" to "Phone",
                ), mockk(relaxed = true)
            )

            assertCheck(vm)

            coVerify {
                driverRepository.setUserData(phoneNumber = "Phone")
                accountRepository.onKeyReceived("key1")
            }
        }

    }

    @Test
    fun `handle new deeplink with driver id`() {
        val driverRepository = mockk<DriverRepository>(relaxed = true)

        createVm(driverRepository = driverRepository).let { vm ->
            vm.handleDeeplink(
                mapOf(
                    "publishable_key" to "key",
                    "email" to "email@mail.com",
                    "phone_number" to "Phone",
                    "driver_id" to "email@mail.com",
                ), mockk(relaxed = true)
            )

            vm.errorHandler.errorText.observeAndAssertNull()

            vm.destination.observeAndGetValue().payload.let {
                assertEquals(
                    SplashScreenFragmentDirections.actionGlobalVisitManagementFragment(),
                    it
                )
            }

            verify {
                driverRepository.setUserData(
                    email = "email@mail.com",
                    phoneNumber = "Phone",
                )
            }
        }
    }

    @Test
    fun `handle new deeplink with metadata`() {
        val driverRepository = mockk<DriverRepository>(relaxed = true)
        val map = mapOf(
            "a" to 1,
            "b" to "c",
            "c" to mapOf(
                "cc" to 1
            )
        )

        fun assertCheck(vm: SplashScreenViewModel) {
            verify {
                driverRepository.setUserData(
                    eq(map),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            }

            vm.errorHandler.errorText.observeAndAssertNull()

            vm.destination.observeAndGetValue().payload.let {
                assertEquals(
                    SplashScreenFragmentDirections.actionGlobalVisitManagementFragment(),
                    it
                )
            }
        }

        createVm(driverRepository = driverRepository).let { vm ->
            vm.handleDeeplink(
                mapOf(
                    "publishable_key" to "key",
                    "email" to "email@mail.com",
                    "metadata" to map,
                ), mockk(relaxed = true)
            )

            assertCheck(vm)
        }

        createVm(driverRepository = driverRepository).let { vm ->
            vm.handleDeeplink(
                mapOf(
                    "publishable_key" to "key",
                    "email" to "email@mail.com",
                    "metadata" to Injector.getMoshi().adapter<Map<String, Any>>(
                        Types.newParameterizedType(
                            Map::class.java, String::class.java,
                            Any::class.java
                        )
                    ).toJson(map),
                ), mockk(relaxed = true)
            )

            assertCheck(vm)
        }
    }


    @Test
    fun `handle deeplink with invalid publishable key`() {
        val slot = slot<Exception>()
        val vm = createVm(slot)

        vm.handleDeeplink(
            mapOf(
                "publishable_key" to "invalid",
                "email" to "email@mail.com",
            ), mockk(relaxed = true)
        )

        assertEquals("Exception: Invalid publishable_key", slot.captured.message)

        vm.errorHandler.errorText.observeAndGetValue().let {
            assertEquals("Exception: Invalid publishable_key", it.value)
        }
        vm.destination.observeAndGetValue().payload.let {
            assertEquals(
                SplashScreenFragmentDirections.actionSplashScreenFragmentToSignInFragment(),
                it
            )
        }
    }

    @Test
    fun `handle deeplink with invalid publishable key (if logged in)`() {
        val slot = slot<Exception>()
        val driverRepository: DriverRepository = mockk(relaxed = true)
        val vm = createVm(slot, driverRepository = driverRepository, loggedIn = true)

        vm.handleDeeplink(
            mapOf(
                "publishable_key" to "invalid",
                "email" to "email@mail.com",
            ), mockk(relaxed = true)
        )

        assertEquals("Exception: Invalid publishable_key", slot.captured.message)

        vm.errorHandler.errorText.observeAndGetValue().let {
            assertEquals("Exception: Invalid publishable_key", it.value)
        }
        vm.destination.observeAndGetValue().payload.let {
            assertEquals(
                SplashScreenFragmentDirections.actionGlobalVisitManagementFragment(),
                it
            )
        }
        verify(exactly = 0) {
            driverRepository.setUserData(allAny())
        }
    }

    @Test
    fun `handle invalid deeplink if already logged in`() {
        val driverRepository = mockk<DriverRepository>(relaxed = true)

        createVm(driverRepository = driverRepository, loggedIn = true).let { vm ->
            vm.handleDeeplink(
                mapOf(
                    "publishable_key" to "key",
                ), mockk(relaxed = true)
            )

            vm.errorHandler.errorText.observeAndGetValue().let {
                assertEquals(R.string.splash_screen_invalid_link.toString(), it.value)
            }

            vm.destination.observeAndGetValue().payload.let {
                assertEquals(
                    SplashScreenFragmentDirections.actionGlobalVisitManagementFragment(),
                    it
                )
            }

            verify(exactly = 0) {
                driverRepository.setUserData(allAny())
            }
        }
    }

    @Test
    fun `handle invalid deeplink with shadowed metadata fields`() {
        val driverRepository = mockk<DriverRepository>(relaxed = true)

        fun assertCheck(vm: SplashScreenViewModel) {
            vm.errorHandler.errorText.observeAndGetValue().let {
                assertEquals(R.string.splash_screen_invalid_link.toString(), it.value)
            }

            vm.destination.observeAndGetValue().payload.let {
                assertEquals(
                    SplashScreenFragmentDirections.actionSplashScreenFragmentToSignInFragment(),
                    it
                )
            }
        }

        createVm(driverRepository = driverRepository).let { vm ->
            vm.handleDeeplink(
                mapOf(
                    "publishable_key" to "key",
                    "email" to "email@mail.com",
                    "metadata" to mapOf(
                        "email" to "email@mail.com"
                    )
                ), mockk(relaxed = true)
            )

            assertCheck(vm)
        }

        createVm(driverRepository = driverRepository).let { vm ->
            vm.handleDeeplink(
                mapOf(
                    "publishable_key" to "key",
                    "phone_number" to "email@mail.com",
                    "metadata" to mapOf(
                        "phone_number" to "email@mail.com"
                    )
                ), mockk(relaxed = true)
            )

            assertCheck(vm)
        }

        createVm(driverRepository = driverRepository).let { vm ->
            vm.handleDeeplink(
                mapOf(
                    "publishable_key" to "key",
                    "driver_id" to "email@mail.com",
                    "metadata" to mapOf(
                        "driver_id" to "email@mail.com"
                    )
                ), mockk(relaxed = true)
            )

            vm.errorHandler.errorText.observeAndAssertNull()

            vm.destination.observeAndGetValue().payload.let {
                assertEquals(
                    SplashScreenFragmentDirections.actionGlobalVisitManagementFragment(),
                    it
                )
            }
        }
    }

    companion object {
        fun createVm(
            exceptionSlot: CapturingSlot<Exception> = slot(),
            loggedIn: Boolean = false,
            crashReportsProvider: CrashReportsProvider = mockk(relaxed = true) {
                every { logException(capture(exceptionSlot)) } returns Unit
            },
            driverRepository: DriverRepository = mockk(relaxed = true),
            accountRepository: AccountRepository = createAccountRepo(loggedIn),
        ) = SplashScreenViewModel(
            mockk(relaxed = true) {
                every { osUtilsProvider } returns mockk(relaxed = true) {
                    every { isEmail(any()) } answers {
                        firstArg<String>().contains("@")
                    }
                    every { stringFromResource(any(), any()) } answers {
                        "${firstArg<Int>()}"
                    }
                    every { stringFromResource(any()) } answers {
                        "${firstArg<Int>()}"
                    }
//                  every { getErrorMessage(any()) } answers {
//                      print("ddd ${firstArg<Any>()}")
//                      (firstArg() as Any).toString()
//                  }
                }
                every { this@mockk.crashReportsProvider } returns crashReportsProvider
            },
            driverRepository,
            accountRepository,
            mockk(relaxed = true),
            Injector.getMoshi(),
        )

        fun createAccountRepo(loggedIn: Boolean): AccountRepository = mockk(relaxed = true) {
            every { isVerifiedAccount } returns loggedIn
            coEvery { onKeyReceived("key", allAny()) } returns true
            coEvery { onKeyReceived("key1", allAny()) } returns true
            coEvery { onKeyReceived("invalid", allAny()) } returns false
        }
    }


}