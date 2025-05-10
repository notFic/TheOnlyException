package org.example.components;

import com.almasb.fxgl.entity.component.Component;
import org.example.interfaces.patterns.IObservable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A component that can be observed for game events.
 * Implements the IObservable interface for standardized event handling.
 */
public class ObservableComponent extends Component implements IObservable<Object> {
    
    private final Map<String, Map<Integer, EventListener<Object>>> eventListeners = new ConcurrentHashMap<>();
    private final AtomicInteger nextListenerId = new AtomicInteger(1);
    
    /**
     * Implementation of IObservable.addEventListener
     * Add a listener for events from this component.
     * 
     * @param eventType Type of event to listen for
     * @param listener Callback to execute when event occurs
     * @return A handle that can be used to remove the listener
     */
    @Override
    public int addEventListener(String eventType, EventListener<Object> listener) {
        int id = nextListenerId.getAndIncrement();
        
        // Get or create the map for this event type
        eventListeners.computeIfAbsent(eventType, k -> new ConcurrentHashMap<>())
                .put(id, listener);
        
        return id;
    }
    
    /**
     * Implementation of IObservable.removeEventListener
     * Remove a listener by its handle.
     * 
     * @param handle Handle returned from addEventListener
     * @return true if the listener was removed
     */
    @Override
    public boolean removeEventListener(int handle) {
        for (Map<Integer, EventListener<Object>> listeners : eventListeners.values()) {
            if (listeners.containsKey(handle)) {
                listeners.remove(handle);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Fire an event to all registered listeners of that event type.
     * 
     * @param eventType Type of event to fire
     * @param eventData Data to pass to listeners (can be null)
     */
    public void fireEvent(String eventType, Object eventData) {
        Map<Integer, EventListener<Object>> listeners = eventListeners.get(eventType);
        
        if (listeners != null) {
            // Create a copy of the listeners to avoid concurrent modification issues
            for (EventListener<Object> listener : listeners.values()) {
                try {
                    listener.onEvent(eventType, eventData);
                } catch (Exception e) {
                    System.err.println("Error in event listener: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Remove all listeners for a specific event type.
     * 
     * @param eventType Event type to clear listeners for
     */
    public void clearEventListeners(String eventType) {
        eventListeners.remove(eventType);
    }
    
    /**
     * Remove all event listeners from this component.
     */
    public void clearAllEventListeners() {
        eventListeners.clear();
    }
    
    /**
     * Check if this component has any listeners for a specific event type.
     * 
     * @param eventType Event type to check
     * @return true if the component has listeners for this event
     */
    public boolean hasListenersForEvent(String eventType) {
        Map<Integer, EventListener<Object>> listeners = eventListeners.get(eventType);
        return listeners != null && !listeners.isEmpty();
    }
} 