package in.zeta.microloan.platform.config;

import com.google.gson.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class GsonConfig {

    @Bean
    public Gson gson() {
        GsonBuilder builder = new GsonBuilder();

        builder.registerTypeAdapter(LocalDate.class, new JsonSerializer<LocalDate>() {
            @Override
            public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
                return new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE));
            }
        });

        builder.registerTypeAdapter(LocalDate.class, new JsonDeserializer<LocalDate>() {
            @Override
            public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                    throws JsonParseException {
                return LocalDate.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE);
            }
        });

        builder.registerTypeAdapter(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
            @Override
            public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
                return new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        });

        builder.registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
            @Override
            public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                    throws JsonParseException {
                return LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        });

        builder.setPrettyPrinting();
        return builder.create();
    }
}
