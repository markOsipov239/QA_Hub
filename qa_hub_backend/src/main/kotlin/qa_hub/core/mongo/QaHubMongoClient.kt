package qa_hub.core.mongo

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoCredential
import org.apache.juli.logging.LogFactory
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Repository

@Repository
class QaHubMongoClient {
    val logger =  LogFactory.getLog("MongoDbLogger")

    private val qaHubDb = "dbQaHub"

    private var mongoHost = System.getenv("ENV_MONGO_QA_HUB_HOST") ?: "mongodb://localhost:27017/"
    private val userName = System.getenv("ENV_MONGO_QA_HUB_LOGIN")
    private val userPass = System.getenv("ENV_MONGO_QA_HUB_PASSWORD")
    private val authSource = System.getenv("ENV_MONGO_QA_HUB_AUTH_SOURCE")

    //Truststore is located in src/main/resources/certs/trustore.jks file.
//    private val trustStoreDir = System.getenv("ENV_MONGO_QA_HUB_TRUST_STORE_DIR") ?: ""
//    private val trustStoreFile = System.getenv("ENV_MONGO_QA_HUB_TRUST_STORE_FILE") ?: ""
//    private val trustStoreFilePath = if (trustStoreFile.isNotBlank() && trustStoreFile.isNotBlank()) {
//        "$trustStoreDir/$trustStoreFile"
//    } else {
//        ClassPathResource("certs/truststore.jks").file.absolutePath
//    }
//    private val trustStorePass = System.getenv("ENV_MONGO_QA_HUB_TRUST_STORE_PASS") ?: "123456"

    val client = run {
        val builder = MongoClientSettings.builder()
        val connectionString = ConnectionString(mongoHost)
        logger.info("Connecting to mongodb: $connectionString")

        builder.applyConnectionString(connectionString)
        builder.retryWrites(false)

        if (!userName.isNullOrEmpty() && !userPass.isNullOrEmpty()) {
            builder.credential(
                MongoCredential.createScramSha1Credential(
                    userName,
                    authSource ?: "admin",
                    userPass.toCharArray()
                )
            )
        }

//        if (!trustStoreFilePath.isNullOrEmpty()) {
//            System.setProperty("javax.net.ssl.trustStore", trustStoreFilePath)
//            System.setProperty("javax.net.ssl.trustStorePassword", trustStorePass)
//
//            builder.applyToSslSettings {
//                it.enabled(true)
//                it.invalidHostNameAllowed(true)
//            }
//        }

        KMongo.createClient(builder.build())
    }

    val db: CoroutineDatabase = client.coroutine.getDatabase(qaHubDb)
}