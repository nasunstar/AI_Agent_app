// ✅ 패키지 경로에서 '.data'를 삭제했습니다.
package com.example.database_project.dao
// 이 파일이 dao 패키지에 속함을 나타냄 (DB 접근 인터페이스)

// Room 관련 어노테이션 불러오기
import androidx.room.*

// ✅ import 경로에서 '.data'를 삭제하여 올바른 User 클래스를 가리키도록 수정했습니다.
import com.example.database_project.entity.User
// User 엔티티(테이블 정의 클래스) 불러오기

import kotlinx.coroutines.flow.Flow
// Flow: 코루틴 기반 비동기 데이터 스트림 → DB 변경이 있으면 자동으로 UI에 반영 가능

@Dao
interface UserDao {
    // @Dao → 이 인터페이스가 Room DAO임을 선언
    // 여기서 정의한 메서드는 Room이 자동으로 SQL 코드로 변환해서 실행

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(user: User): Long
    // 새로운 User 데이터 삽입
    // onConflict = IGNORE → 동일한 PK(혹은 유니크 컬럼)가 있을 경우 무시하고 삽입 실패(0 반환)
    // Long 반환값 → 삽입된 행(row)의 id(PK) 반환

    @Update
    suspend fun update(user: User)
    // 기존 User 데이터 갱신 (id가 동일한 레코드를 찾아 업데이트)

    @Delete
    suspend fun delete(user: User)
    // 특정 User 데이터를 삭제 (id를 기준으로 삭제됨)

    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAll(): Flow<List<User>>
    // 모든 사용자 데이터를 이름순(오름차순)으로 가져옴
    // Flow<List<User>> → DB 변경 시 자동 업데이트됨 (UI에 실시간 반영 가능)

    // ✅ 이름으로 특정 유저를 찾는 기능을 추가했습니다. (정규화에 필요)
    @Query("SELECT * FROM users WHERE name = :name LIMIT 1")
    suspend fun getUserByName(name: String): User?
    // name 값으로 User를 검색
    // LIMIT 1 → 결과가 여러 개여도 첫 번째만 반환
    // User? → 결과가 없을 경우 null 반환
}
