package com.example.rural.repository;

import com.example.rural.entity.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketTypeRepository extends JpaRepository<TicketType, Long> {

    /** 景点的所有票型（上架的排前面） */
    List<TicketType> findBySpotIdOrderByEnabledDescIdAsc(Long spotId);

    /** 景点的上架票型 */
    List<TicketType> findBySpotIdAndEnabledTrue(Long spotId);

    /** 同一景点是否已有同名票型 */
    boolean existsBySpotIdAndName(Long spotId, String name);

    boolean existsBySpotIdAndNameAndIdNot(Long spotId, String name, Long excludeId);
}
