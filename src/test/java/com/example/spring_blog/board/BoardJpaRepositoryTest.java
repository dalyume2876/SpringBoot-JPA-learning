//package com.example.spring_blog.board;
//
//import com.example.spring_blog.user.User;
//import jakarta.persistence.EntityManager;
//import org.hibernate.Hibernate;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
//import java.util.List;
//import static org.assertj.core.api.Assertions.assertThat;
//
//@DataJpaTest(properties = "spring.sql.init.mode=never")
//class BoardJpaRepositoryTest {
//    @Autowired
//    private BoardJpaRepository boardJpaRepository;
//    @Autowired
//    private EntityManager entityManager;
//    private User author;
//
//    @BeforeEach
//    void setUp() {
//        author = new User("author", "1234", "author@example.com");
//        entityManager.persist(author);
//    }
//
//    @Test
//    @DisplayName("게시글 저장 시 ID, 생성 시간과 작성자 관계가 유지된다")
//    void save_test() {
//        Board saved = boardJpaRepository.save(new Board("title", "content", author));
//        flushAndClear();
//        Board found = boardJpaRepository.findById(saved.getId()).orElseThrow();
//        assertThat(found.getId()).isPositive();
//        assertThat(found.getTitle()).isEqualTo("title");
//        assertThat(found.getContent()).isEqualTo("content");
//        assertThat(found.getCreatedAt()).isNotNull();
//        assertThat(found.getUser().getId()).isEqualTo(author.getId());
//    }
//
//    @Test
//    @DisplayName("없는 게시글 ID 조회는 빈 Optional을 반환한다")
//    void findById_notFound_test() {
//        assertThat(boardJpaRepository.findById(Long.MAX_VALUE)).isEmpty();
//        assertThat(boardJpaRepository.findByIdJoinUser(Long.MAX_VALUE)).isEmpty();
//    }
//
//    @Test
//    @DisplayName("전체 게시글, 개수 및 ID 존재 여부를 조회한다")
//    void findAll_test() {
//        Board first = boardJpaRepository.save(new Board("first", "content1", author));
//        Board second = boardJpaRepository.save(new Board("second", "content2", author));
//        flushAndClear();
//        assertThat(boardJpaRepository.findAll()).extracting(Board::getId)
//                .containsExactlyInAnyOrder(first.getId(), second.getId());
//        assertThat(boardJpaRepository.count()).isEqualTo(2);
//        assertThat(boardJpaRepository.existsById(first.getId())).isTrue();
//        assertThat(boardJpaRepository.existsById(Long.MAX_VALUE)).isFalse();
//    }
//
//    @Test
//    @DisplayName("변경 감지로 게시글 수정 내용이 DB에 반영된다")
//    void update_test() {
//        Long id = boardJpaRepository.save(new Board("title", "content", author)).getId();
//        flushAndClear();
//        BoardRequest.UpdateDTO request = new BoardRequest.UpdateDTO();
//        request.setTitle("updated title");
//        request.setContent("updated content");
//        boardJpaRepository.findById(id).orElseThrow().update(request);
//        flushAndClear();
//        Board found = boardJpaRepository.findById(id).orElseThrow();
//        assertThat(found.getTitle()).isEqualTo("updated title");
//        assertThat(found.getContent()).isEqualTo("updated content");
//        assertThat(found.getUser().getId()).isEqualTo(author.getId());
//    }
//
//    @Test
//    @DisplayName("게시글 삭제 시 작성자는 유지된다")
//    void deleteById_test() {
//        Long id = boardJpaRepository.save(new Board("title", "content", author)).getId();
//        flushAndClear();
//        boardJpaRepository.deleteById(id);
//        flushAndClear();
//        assertThat(boardJpaRepository.findById(id)).isEmpty();
//        assertThat(entityManager.find(User.class, author.getId())).isNotNull();
//    }
//
//    @Test
//    @DisplayName("단건 fetch join은 작성자를 즉시 로딩한다")
//    void findByIdJoinUser_test() {
//        Long id = boardJpaRepository.save(new Board("title", "content", author)).getId();
//        flushAndClear();
//        Board found = boardJpaRepository.findByIdJoinUser(id).orElseThrow();
//        assertThat(Hibernate.isInitialized(found.getUser())).isTrue();
//        entityManager.clear();
//        assertThat(found.getUser().getUsername()).isEqualTo("author");
//        assertThat(found.getTitle()).isEqualTo("title");
//    }
//
//    @Test
//    @DisplayName("전체 fetch join은 ID 내림차순이며 각 작성자를 즉시 로딩한다")
//    void findAllJoinUser_test() {
//        User other = new User("other", "1234", "other@example.com");
//        entityManager.persist(other);
//        Board first = boardJpaRepository.save(new Board("first", "content1", author));
//        Board second = boardJpaRepository.save(new Board("second", "content2", other));
//        Board third = boardJpaRepository.save(new Board("third", "content3", author));
//        flushAndClear();
//        List<Board> boards = boardJpaRepository.findAllJoinUser();
//        assertThat(boards).extracting(Board::getId)
//                .containsExactly(third.getId(), second.getId(), first.getId());
//        assertThat(boards).allSatisfy(board ->
//                assertThat(Hibernate.isInitialized(board.getUser())).isTrue());
//        entityManager.clear();
//        assertThat(boards).extracting(board -> board.getUser().getUsername())
//                .containsExactly("author", "other", "author");
//    }
//
//    @Test
//    @DisplayName("게시글이 없으면 전체 fetch join은 빈 목록을 반환한다")
//    void findAllJoinUser_empty_test() {
//        assertThat(boardJpaRepository.findAllJoinUser()).isEmpty();
//    }
//
//    private void flushAndClear() {
//        entityManager.flush();
//        entityManager.clear();
//    }
//}
