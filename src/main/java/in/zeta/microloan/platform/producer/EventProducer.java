package in.zeta.microloan.platform.producer;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import in.zeta.oms.atropos.client.AtroposPublisherClient;
import in.zeta.oms.atropos.model.PublishMode;
import in.zeta.oms.atropos.response.PublishEventResponse;
import in.zeta.spectra.capture.SpectraLogger;
import olympus.pubsub.model.OperationType;
import olympus.pubsub.model.PubSubEvent;
import olympus.pubsub.model.TopicScope;
import olympus.trace.OlympusSpectra;
import org.apache.http.NameValuePair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletionStage;

import static java.util.UUID.randomUUID;

@Component
public class EventProducer {

    private static final SpectraLogger logger = OlympusSpectra.getLogger(EventProducer.class);

    private final PublishMode publishMode;
    private final AtroposPublisherClient atroposPublisherClient;
    private final Gson gson;

    public EventProducer(
            AtroposPublisherClient atroposPublisherClient,
            Gson gson,
            @Value("${atropos.publish.mode}") String publishModeString
    ) {
        this.atroposPublisherClient = atroposPublisherClient;
        this.gson = gson;

        try {
            this.publishMode = PublishMode.valueOf(publishModeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid publish mode: " + publishModeString).log();
            throw new IllegalArgumentException("Invalid publish mode: " + publishModeString, e);
        } catch (Exception e) {
            logger.error("Unexpected error while getting publish mode: " + e.getMessage(), e).log();
            throw new RuntimeException("Unexpected error while getting publish mode"+ e);
        }
    }

    public CompletionStage<PublishEventResponse> publishEvent(String eventData, String topic) {
        PubSubEvent.Builder builder = buildEvent(eventData, topic, TopicScope.SYSTEM);
        logger.info("Publishing event to topic: " + topic)
                .attr("eventId", randomUUID().toString())
                .attr("topic", topic)
                .attr("eventData", eventData)
                .log();
        return atroposPublisherClient.publish(builder, publishMode).thenApply(response -> {
            logger.info("Publish response: " + response.toString()).log();
            return response;
        }).exceptionally(e -> {
            logger.error("Error occurred while publishing Event")
                    .attr("MESSAGE", e.toString())
                    .attr("REQUEST_BODY", eventData)
                    .log();
            throw new RuntimeException(e.getMessage());
        });
    }

    private PubSubEvent.Builder buildEvent(String eventData, String topic, TopicScope topicScope) {

        return new PubSubEvent.Builder()
                .tenant("0")
                .topicScope(topicScope)
                .objectType(topic)
                .objectID("1")
                .operationType(OperationType.CREATED)
                .sourceAttributes(new NameValuePair[0])
                .tags(List.of())
                .stateMachineState("default")
                .data(gson.fromJson(eventData, JsonElement.class));
    }
}

