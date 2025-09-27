// data/dao/NoteDao.kt
package com.example.database_project.data.dao
// 이 파일이 data/dao 패키지에 포함됨을 나타냄 (DB 접근 전용 인터페이스)

// Room 라이브러리 관련 어노테이션 불러오기
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

// Note 엔티티 클래스 불러오기 (테이블 정의)
import com.example.database_project.data.entity.Note

// Flow: 코루틴 기반 비동기 데이터 스트림
// DB 변경 시 자동으로 새로운 데이터를 방출해 UI 업데이트 가능
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    // @Dao → 이 인터페이스가 DAO임을 나타냄
    // Room이 내부적으로 SQL 코드로 변환해 실행

    @Insert
    suspend fun insert(note: Note): Long
    // 새로운 Note 엔티티를 DB에 삽입
    // suspend → 코루틴 환경에서 비동기 실행 가능
    // Long 반환값 → 삽입된 행(row)의 자동 생성된 id(PK)를 돌려줌

    @Query("SELECT * FROM notes ORDER BY id DESC")  // ★ notes 테이블명과 일치해야 함
    fun getAll(): Flow<List<Note>>
    // notes 테이블의 모든 데이터를 id 내림차순으로 가져옴 (최근 데이터 우선)
    // Flow<List<Note>> → DB 변경이 발생할 때마다 자동으로 새로운 리스트 방출
    // 따라서 UI는 실시간으로 최신 데이터를 반영할 수 있음
}
