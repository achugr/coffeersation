import com.achugr.coffeersation.model.IntroFrequency
import com.achugr.coffeersation.service.TriggerTalkRound
import com.achugr.coffeersation.service.RequestInfo
import com.achugr.coffeersation.service.InitCoffeeTalk
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class AsyncTaskSerializationTest : StringSpec({
    val objectMapper = jacksonObjectMapper()

    "ChannelTrigger should be serialized and deserialized correctly" {
        val triggerTalkRound = TriggerTalkRound("testChannel", 1)
        val serialized = objectMapper.writeValueAsString(triggerTalkRound)
        val deserialized: TriggerTalkRound = objectMapper.readValue(serialized)

        deserialized shouldBe triggerTalkRound
    }

    "InitCoffeeTalkRequest should be serialized and deserialized correctly" {
        val initCoffeeTalk = InitCoffeeTalk("coffeeChannel", IntroFrequency.MONDAY_ONCE_A_WEEK)
        val serialized = objectMapper.writeValueAsString(initCoffeeTalk)
        val deserialized: InitCoffeeTalk = objectMapper.readValue(serialized)

        deserialized shouldBe initCoffeeTalk
    }

    "InfoRequest should be serialized and deserialized correctly" {
        val requestInfo = RequestInfo("infoChannel")
        val serialized = objectMapper.writeValueAsString(requestInfo)
        val deserialized: RequestInfo = objectMapper.readValue(serialized)

        deserialized shouldBe requestInfo
    }
})