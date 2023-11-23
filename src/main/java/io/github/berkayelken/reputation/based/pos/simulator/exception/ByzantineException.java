package io.github.berkayelken.reputation.based.pos.simulator.exception;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ByzantineException extends RuntimeException {
	private static final String LOCK_MESSAGE = "Chain fork is locked when attempt no is %d";
	private final String message;
	private final long attemptNo;

	public ByzantineException(long attemptNo) {
		super();
		this.attemptNo = attemptNo;
		message = String.format(LOCK_MESSAGE, attemptNo);
	}

}
