package com.minecraftly.core.bungee.handlers.module.tpa;

import java.util.UUID;

/**
 * Contains data about a tpa request.
 */
public class TpaData {

    public enum Direction {
        TO_SENDER("invites you to teleport to them."),
        TO_RECEIVER("wants to teleport to you.");

        private String inviteMessage;

        Direction(String inviteMessage) {
            this.inviteMessage = inviteMessage;
        }

        public String getInviteMessage() {
            return inviteMessage;
        }
    }

    private UUID sender;
    private UUID target;
    private Direction direction;
    private long timeCreated;

    public TpaData(UUID initiator, UUID target, Direction direction) {
        this(initiator, target, direction, System.currentTimeMillis());
    }

    public TpaData(UUID initiator, UUID target, Direction direction, long timeCreated) {
        this.sender = initiator;
        this.target = target;
        this.direction = direction;
        this.timeCreated = timeCreated;
    }

    public UUID getInitiatorActor() {
        return sender;
    }

    public UUID getTargetActor() {
        return target;
    }

    public Direction getDirection() {
        return direction;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public UUID getMovingActor() {
        switch (getDirection()) {
            case TO_RECEIVER: return getInitiatorActor();
            case TO_SENDER: return getTargetActor();
            default: throw new IllegalStateException("Unexpected direction.");
        }
    }

    public UUID getDestinationActor() {
        switch (getDirection()) {
            case TO_RECEIVER: return getTargetActor();
            case TO_SENDER: return getInitiatorActor();
            default: throw new IllegalStateException("Unexpected direction.");
        }
    }
}
