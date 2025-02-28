package com.stress.services;

import com.stress.entity.Event;
import com.stress.repository.EventRepository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    // Create a new event
    public Event createEvent(Event event) {
        return eventRepository.save(event);
    }

    // Find event by event code
    public Event findByEventCode(String eventCode) {
        return eventRepository.findByEventCode(eventCode);
    }
    
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }
    public void deleteEvent(Long id) {
        eventRepository.deleteById(id);
    }
}