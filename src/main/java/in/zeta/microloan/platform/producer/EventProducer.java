package in.zeta.microloan.platform.producer;

import com.google.gson.Gson;
import in.zeta.oms.atropos.client.AtroposPublisherClient;
import in.zeta.oms.atropos.model.PublishMode;
import in.zeta.oms.atropos.response.PublishEventResponse;
import in.zeta.spectra.capture.SpectraLogger;
import olympus.pubsub.model.EventMetaData;
import olympus.pubsub.model.OperationType;
import olympus.pubsub.model.PubSubEvent;
import olympus.pubsub.model.TopicScope;
import olympus.trace.OlympusSpectra;
import org.apache.http.NameValuePair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

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
        }
    }

    public CompletionStage<PublishEventResponse> publishEvent(
            String objectId,
            String topic,
            Map<String, Object> data,
            TopicScope topicScope
    ) {
        PubSubEvent.Builder builder = new PubSubEvent.Builder()
                .tenant("0")
                .topicScope(topicScope)
                .objectType(topic)
                .objectID(objectId)
                .operationType(OperationType.CREATED)
                .sourceAttributes(new NameValuePair[0])
                .tags(List.of())
                .stateMachineState("default")
                .data(gson.toJsonTree(data));
        return atroposPublisherClient.publish(builder, publishMode);
    }

    public CompletionStage<PublishEventResponse> publishScheduledEvent(
            String objectId,
            String objectType,
            OperationType operationType,
            Map<String, Object> data,
            TopicScope topicScope,
            int delayMinutes
    ) {
        String scheduledAt = ZonedDateTime.now()
                .plusMinutes(delayMinutes)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));

        EventMetaData metaData = EventMetaData.builder()
                .schedulingType(EventMetaData.SchedulingType.ONCE)
                .scheduledAt(scheduledAt)
                .build();

        PubSubEvent.Builder builder = new PubSubEvent.Builder()
                .tenant("0")
                .topicScope(topicScope)
                .objectType(objectType)
                .objectID(objectId)
                .operationType(operationType)
                .sourceAttributes(new NameValuePair[0])
                .tags(List.of())
                .stateMachineState("default")
                .metaData(metaData)
                .data(gson.toJsonTree(data));
        logger.info("Publish result: " + atroposPublisherClient.publish(builder, publishMode)).log();
        return atroposPublisherClient.publish(builder, publishMode);
    }
}
