package com.example.rural.repository;

import com.example.rural.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    /** 用户的所有地址，默认地址排前面，再按更新时间降序 */
    List<Address> findByUserIdOrderByIsDefaultDescUpdatedAtDesc(Long userId);

    /** 查找用户的默认地址 */
    Optional<Address> findByUserIdAndIsDefaultTrue(Long userId);

    /** 用户的地址数量（用于限制上限） */
    long countByUserId(Long userId);

    /** 将用户的所有地址设为非默认（设新默认前先清除旧的） */
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.userId = :userId")
    void clearDefaultByUserId(Long userId);
}
