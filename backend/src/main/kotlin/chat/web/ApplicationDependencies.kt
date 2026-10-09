package chat.web

import chat.web.auth.GuestIdentityRepository
import chat.web.auth.GuestIdentityService
import chat.web.database.migrateDatabase
import chat.web.database.PostgresGuestIdentityRepository
import chat.web.database.createDataSource
import chat.web.database.databaseSettings
import chat.web.database.PostgresMessageRepository
import chat.web.messages.InMemoryMessageService
import chat.web.messages.MessageContract
import chat.web.messages.PostgresMessageService
import chat.web.rooms.InMemoryRoomService
import chat.web.rooms.RoomContract

/** Application composition root. Replace adapters here without coupling routes to storage. */
data class ApplicationDependencies(
    val guestIdentityRepository: GuestIdentityRepository,
    val guestIdentityService: GuestIdentityService,
    val roomService: RoomContract,
    val messageService: MessageContract
)

fun configureDependencies(
    guestIdentityRepository: GuestIdentityRepository? = null,
    environment: Map<String, String> = System.getenv()
): ApplicationDependencies {
    val dataSource = if (guestIdentityRepository == null) {
        val settings = databaseSettings(environment)
        createDataSource(settings)
    } else null
    if (dataSource != null) {
        migrateDatabase(dataSource)
    }
    val repository = guestIdentityRepository ?: PostgresGuestIdentityRepository(dataSource!!)
    val roomService = InMemoryRoomService()
    return ApplicationDependencies(
        guestIdentityRepository = repository,
        guestIdentityService = GuestIdentityService(repository),
        roomService = roomService,
        messageService = if (dataSource == null) InMemoryMessageService(roomService)
        else PostgresMessageService(roomService, PostgresMessageRepository(dataSource))
    )
}
