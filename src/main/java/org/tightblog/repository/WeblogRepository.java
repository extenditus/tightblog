/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tightblog.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.tightblog.pojos.Weblog;

import java.util.List;

@Repository
@Transactional(value = "transactionManager")
public interface WeblogRepository extends JpaRepository<Weblog, String> {

    Weblog findByHandleAndVisibleTrue(String handle);

    Weblog findByHandle(String handle);

    List<Weblog> findByVisibleTrue(Pageable pageable);

    // return most popular weblogs based on today's hits
    List<Weblog> findByVisibleTrueAndHitsTodayGreaterThanOrderByHitsTodayDesc(int hitsToday, Pageable pageable);

    long count();

    default Weblog findByIdOrNull(String id) {
        return findById(id).orElse(null);
    }

    @Query("SELECT w FROM Weblog w WHERE UPPER(w.handle) like %?1 ORDER BY w.handle")
    List<Weblog> findByLetterOrderByHandle(char firstLetter, Pageable pageable);

    @Query("SELECT COUNT(w) FROM Weblog w WHERE UPPER(w.handle) like %?1")
    int getCountByHandle(char firstLetter);

    @Modifying
    @Query("UPDATE Weblog w SET w.hitsToday = 0, w.lastModified = CURRENT_TIMESTAMP")
    int updateDailyHitCountZero();

}
