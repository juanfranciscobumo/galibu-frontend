package com.galibu.core

import com.galibu.core.data.fake.FakeAppNotificationDao
import com.galibu.core.data.fake.FakeBidDao
import com.galibu.core.data.fake.FakeChatMessageDao
import com.galibu.core.data.fake.FakeJobRequestDao
import com.galibu.core.data.fake.FakeUserProfileDao
import com.galibu.core.data.model.AppNotification
import com.galibu.core.data.model.Bid
import com.galibu.core.data.model.ChatMessage
import com.galibu.core.data.model.JobRequest
import com.galibu.core.data.model.UserProfile
import io.mockk.mockk
import com.galibu.core.data.firebase.FirebaseService
import com.galibu.core.data.remote.ApiService
import com.galibu.core.data.remote.InMemoryTokenStorage
import com.galibu.core.data.remote.TokenStore
import com.galibu.core.data.repository.GalibuRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before

@OptIn(ExperimentalCoroutinesApi::class)
abstract class BaseRepositoryTest {

    protected val testDispatcher = StandardTestDispatcher()

    protected lateinit var fakeJobDao: FakeJobRequestDao
    protected lateinit var fakeBidDao: FakeBidDao
    protected lateinit var fakeUserDao: FakeUserProfileDao
    protected lateinit var fakeChatDao: FakeChatMessageDao
    protected lateinit var fakeNotificationDao: FakeAppNotificationDao
    protected lateinit var mockFirebase: FirebaseService
    protected lateinit var mockApi: ApiService
    protected lateinit var tokenStore: TokenStore
    protected lateinit var repository: GalibuRepository

    @Before
    open fun setUp() {
        Dispatchers.setMain(testDispatcher)

        fakeJobDao = FakeJobRequestDao()
        fakeBidDao = FakeBidDao()
        fakeUserDao = FakeUserProfileDao()
        fakeChatDao = FakeChatMessageDao()
        fakeNotificationDao = FakeAppNotificationDao()
        mockFirebase = mockk(relaxed = true)
        mockApi = mockk(relaxed = true)
        tokenStore = TokenStore(InMemoryTokenStorage())

        repository = GalibuRepository(
            userProfileDao = fakeUserDao,
            jobRequestDao = fakeJobDao,
            bidDao = fakeBidDao,
            chatMessageDao = fakeChatDao,
            notificationDao = fakeNotificationDao,
            firebaseService = mockFirebase,
            apiService = mockApi,
            tokenStore = tokenStore
        )
    }

    @After
    open fun tearDown() {
        Dispatchers.resetMain()
    }

    protected fun createJob(
        id: String = "job_1",
        category: String = "Plomería",
        title: String = "Reparación de grifo",
        description: String = "Fuga en cocina",
        status: String = "OPEN",
        clientId: String = "client_1",
        clientName: String = "Juan Pérez",
        professionalId: String? = null,
        professionalName: String? = null,
        budgetMin: Double = 50000.0,
        counterOfferAmount: Double? = null,
        counterOfferSender: String? = null,
        address: String = "Calle 123",
        createdAt: Long = System.currentTimeMillis()
    ) = JobRequest(
        id = id, category = category, title = title, description = description,
        status = status, clientId = clientId, clientName = clientName,
        professionalId = professionalId, professionalName = professionalName,
        budgetMin = budgetMin, counterOfferAmount = counterOfferAmount,
        counterOfferSender = counterOfferSender, address = address, createdAt = createdAt
    )

    protected fun createBid(
        id: String = "bid_1",
        jobId: String = "job_1",
        professionalId: String = "pro_1",
        professionalName: String = "Carlos García",
        amount: Double = 60000.0,
        comment: String = "Puedo hacerlo rápido",
        durationHours: Int = 2,
        status: String = "PENDING"
    ) = Bid(
        id = id, jobId = jobId, professionalId = professionalId,
        professionalName = professionalName, amount = amount,
        comment = comment, durationHours = durationHours, status = status
    )

    protected fun createUser(
        id: String = "pro_1",
        name: String = "Carlos García",
        email: String = "carlos@test.com",
        role: String = "PROFESSIONAL",
        rating: Float = 4.8f,
        completedJobs: Int = 25,
        balance: Double = 500000.0
    ) = UserProfile(
        id = id, name = name, email = email, role = role,
        rating = rating, completedJobs = completedJobs, balance = balance
    )

    protected fun createNotification(
        userId: String = "user_1",
        title: String = "Test",
        body: String = "Body"
    ) = AppNotification(userId = userId, title = title, body = body)
}
