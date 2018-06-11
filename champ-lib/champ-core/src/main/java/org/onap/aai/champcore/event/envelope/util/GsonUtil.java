/**
 * ============LICENSE_START==========================================
 * org.onap.aai
 * ===================================================================
 * Copyright © 2017 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017 Amdocs
 * ===================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END============================================
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */
package org.onap.aai.champcore.event.envelope.util;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class GsonUtil {

    /**
     * All methods are static.
     */
    private GsonUtil() {
        // Do not instantiate
    }

    /**
     * Tell Gson how to handle Optional fields. This factory builds a Type Adapter for a class wrapped
     * with Optional
     *
     */
    public static class OptionalTypeAdapterFactory implements TypeAdapterFactory {
        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            Class<T> rawType = (Class<T>) type.getRawType();
            if (rawType != Optional.class) {
                return null;
            }
            final ParameterizedType parameterizedType = (ParameterizedType) type.getType();
            final Type actualType = parameterizedType.getActualTypeArguments()[0];
            return new OptionalTypeAdapter(gson.getAdapter(TypeToken.get(actualType)));
        }
    }

    /**
     * Implementation of the Optional Type Adapter
     *
     * @param <E>
     */
    public static class OptionalTypeAdapter<E> extends TypeAdapter<Optional<E>> {

        private final TypeAdapter<E> adapter;

        public static final TypeAdapterFactory FACTORY = new OptionalTypeAdapterFactory();

        /**
         * @param adapter
         */
        public OptionalTypeAdapter(TypeAdapter<E> adapter) {
            this.adapter = adapter;
        }

        @Override
        public void write(JsonWriter out, Optional<E> value) throws IOException {
            if (value != null && value.isPresent()) { // NOSONAR
                adapter.write(out, value.get());
            } else {
                boolean nullsAllowed = out.getSerializeNulls();
                if (nullsAllowed) {
                    out.setSerializeNulls(false);
                }
                out.nullValue();
                if (nullsAllowed) {
                    out.setSerializeNulls(true);
                }
            }
        }

        @Override
        public Optional<E> read(JsonReader in) throws IOException {
            final JsonToken peek = in.peek();
            if (peek != JsonToken.NULL) {
                return Optional.ofNullable(adapter.read(in));
            }
            return Optional.empty();
        }

    }

    /**
     * @return a new GsonBuilder with standard settings
     */
    public static GsonBuilder createGsonBuilder() {
        return new GsonBuilder().disableHtmlEscaping().setPrettyPrinting()
                .registerTypeAdapterFactory(OptionalTypeAdapter.FACTORY);
    }

    /**
     * @return a new Gson instance
     */
    public static Gson createGson() {
        return createGsonBuilder().create();
    }
}
