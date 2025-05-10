package org.example.interfaces.patterns;

/**
 * Interface for objects that can be observed (publish events).
 * Uses a simple callback-based event system.
 */
public interface IObservable<T> {
    /**
     * Add a listener for events from this observable.
     * @param eventType Type of event to listen for
     * @param listener Callback to execute when event occurs
     * @return A handle that can be used to remove the listener
     */
    int addEventListener(String eventType, EventListener<T> listener);
    
    /**
     * Remove a listener by its handle.
     * @param handle Handle returned from addEventListener
     * @return true if the listener was removed
     */
    boolean removeEventListener(int handle);
    
    /**
     * Functional interface for event listeners.
     */
    @FunctionalInterface
    interface EventListener<T> {
        void onEvent(String eventType, T eventData);
    }
} 