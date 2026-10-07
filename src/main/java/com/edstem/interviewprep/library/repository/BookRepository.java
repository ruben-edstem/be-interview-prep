package com.edstem.interviewprep.library.repository;

import com.edstem.interviewprep.library.entity.Book;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

  @Query(
      """
      select b from Book b
      where lower(b.title) like lower(concat('%', :term, '%'))
         or lower(b.author) like lower(concat('%', :term, '%'))
      order by b.id
      """)
  List<Book> search(@Param("term") String term);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from Book b where b.id = :id and b.borrowed = false")
  int deleteIfAvailable(@Param("id") Long id);
}
