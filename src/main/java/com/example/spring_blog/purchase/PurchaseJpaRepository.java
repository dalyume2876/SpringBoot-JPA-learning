package com.example.spring_blog.purchase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PurchaseJpaRepository extends JpaRepository<Purchase, Long> {
    //    구메 내역 조회
    @Query("SELECT p FROM Purchase p WHERE p.user.id = :userId AND p.board.id = :boardId")
    Optional<Purchase> findByUserIdAndBoardId(@Param("userId") Long userId, @Param("boardId") Long boardId);


    //    구매 여부 확인 (true, false)응답 필요시
    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
            FROM Purchase p
            WHERE p.user.id = :userId AND p.board.id = :boardId
            """)
    boolean existsByUserIdAndBoardId(@Param("userId") Long userId, @Param("boardId") Long boardId);

    @Query("SELECT COUNT(p) FROM Purchase p WHERE p.board.id = :boardId")
    long countByBoardId(@Param("boardId") Long boardId);

    //    게시글에 달린 구매내역 일괄삭제
    @Modifying // select가 아니면 반드시 사용해야함
    @Query("DELETE FROM Purchase p WHERE p.board.id = :boardId")
    void deleteByBoardId(@Param("boardId") Long boardId);
}
