package com.stress.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.stress.entity.Event;

public interface EventRepository extends JpaRepository<Event, Long> {
    Event findByEventCode(String eventCode);
}