// ✅ 패키지 경로에서 '.data'를 삭제했습니다.
package com.example.database_project.dao
// 이 파일이 dao 패키지 안에 있음을 명시 (DAO = Data Access Object, DB 접근 인터페이스)

// Room 관련 어노테이션 불러오기
import androidx.room.*

// ✅ import 경로에서 '.data'를 삭제하여 올바른 Event 클래스를 가리키도록 수정했습니다.
import com.example.database_project.entity.Event
// Event 테이블(Entity 클래스)을 불러옴

import kotlinx.coroutines.flow.Flow
// Flow: 코루틴 기반의 비동기 데이터 스트림 (DB 변경 시 자동으로 업데이트 받을 수 있음)

@Dao
interface EventDao {
    // @Dao: 이 인터페이스가 Room DAO임을 선언
    // 여기서 정의한 메서드들은 Room이 내부적으로 SQL 코드로 변환해 실행함

    @Insert
    suspend fun insert(event: Event): Long
    // 새로운 이벤트 데이터를 DB에 삽입
    // suspend → 코루틴에서 실행 가능 (비동기 처리)
    // Long 반환 → 삽입된 행(row)의 id(PK)를 반환

    @Update
    suspend fun update(event: Event)
    // 기존 이벤트 데이터를 수정
    // Event 객체의 id(primary key)가 기준이 됨

    @Delete
    suspend fun delete(event: Event)
    // 기존 이벤트 데이터를 삭제
    // Event 객체의 id(primary key)가 기준이 됨

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Event?
    // 특정 id를 가진 이벤트 1개만 가져오기
    // :id → 메서드 매개변수 id 값이 SQL에 바인딩됨
    // Event? → 없을 경우 null 반환

    // ❗️'event_timestamp'는 Event Entity에 'timestamp'로 정의했으므로, 이름을 맞춰주어야 합니다.
    @Query("SELECT * FROM events ORDER BY timestamp DESC")
    fun getAll(): Flow<List<Event>>
    // 모든 이벤트를 가져오기 (최신순 정렬)
    // Flow<List<Event>> → 비동기 스트림으로 반환
    // DB 변경 발생 시 자동으로 새로운 값 방출 (UI에 실시간 반영 가능)
}
