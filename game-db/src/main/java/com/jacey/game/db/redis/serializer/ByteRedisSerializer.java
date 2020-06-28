package com.jacey.game.db.redis.serializer;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.Charset;

public class ByteRedisSerializer implements RedisSerializer<byte[]> {

	public ByteRedisSerializer() {
		this(Charset.forName("UTF8"));
	}

	public ByteRedisSerializer(Charset charset) {
	}

	@Override
	public byte[] serialize(byte[] t) throws SerializationException {
		return t;
	}

	@Override
	public byte[] deserialize(byte[] bytes) throws SerializationException {
		return bytes;
	}

}
