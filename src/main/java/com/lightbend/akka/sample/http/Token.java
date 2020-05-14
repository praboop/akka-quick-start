package com.lightbend.akka.sample.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class Token {
	@JsonProperty("token")
	private String token;

	public Token() {
	}

	public Token(String t) {
		token = t;
	}

	public String getToken() {
		return token;
	}

}
