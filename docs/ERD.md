```mermaid
erDiagram
    %% Restaurant Domain
    RESTAURANT {
        bigint id PK
        varchar name "맛집명"
        varchar category "카테고리"
        varchar address "주소"
        varchar phone "전화번호"
        decimal rating "평점"
        int review_count "리뷰수"
        varchar external_id "외부API ID"
        varchar source "데이터 소스(naver/kakao)"
        timestamp created_at
        timestamp updated_at
    }

    SEARCH_REQUEST {
        bigint id PK
        varchar keyword "검색 키워드"
        varchar location "지역 정보"
        varchar sort_type "정렬 방식"
        int page "페이지 번호"
        int size "페이지 크기"
        varchar session_id "세션 ID"
        timestamp created_at
    }

    SEARCH_RESULT {
        bigint id PK
        bigint search_request_id FK
        bigint restaurant_id FK
        int rank_order "검색 결과 순서"
        decimal relevance_score "관련도 점수"
        varchar source "데이터 소스"
        timestamp created_at
    }

    %% Keyword Domain
    KEYWORD {
        bigint id PK
        varchar keyword "키워드"
        varchar normalized_keyword "정규화된 키워드"
        timestamp created_at
        timestamp updated_at
    }

    KEYWORD_COUNT {
        bigint id PK
        bigint keyword_id FK
        bigint location_category_id FK
        bigint count "검색 횟수"
        date count_date "집계 날짜"
        timestamp updated_at
    }

    SEARCH_RECORD {
        bigint id PK
        bigint search_request_id FK
        bigint keyword_id FK
        bigint location_category_id FK
        varchar session_id "세션 ID"
        varchar user_agent "사용자 에이전트"
        varchar ip_address "IP 주소"
        timestamp created_at
    }

    POPULAR_KEYWORD {
        bigint id PK
        bigint keyword_id FK
        bigint location_category_id FK
        bigint count "검색 횟수"
        int rank_order "순위"
        date ranking_date "순위 날짜"
        timestamp created_at
        timestamp updated_at
    }

    %% External API Integration
    API_CALL_LOG {
        bigint id PK
        bigint api_config_id FK
        varchar endpoint "API 엔드포인트"
        varchar request_params "요청 파라미터"
        int response_status "응답 상태"
        int response_time_ms "응답시간(ms)"
        varchar error_message "에러 메시지"
        timestamp created_at
    }

    EXTERNAL_API_CONFIG {
        bigint id PK
        varchar api_name "API명"
        varchar base_url "기본 URL"
        varchar client_id "클라이언트 ID"
        varchar client_secret "클라이언트 시크릿"
        int daily_limit "일일 한도"
        int current_usage "현재 사용량"
        boolean is_active "활성화 여부"
        timestamp last_reset_at "마지막 리셋 시간"
        timestamp created_at
        timestamp updated_at
    }

    %% Search Strategy & Event
    SEARCH_EVENT {
        bigint id PK
        bigint search_request_id FK
        varchar event_type "이벤트 타입"
        varchar metadata "추가 메타데이터(JSON)"
        boolean processed "처리 여부"
        timestamp created_at
        timestamp processed_at
    }

    SEARCH_STRATEGY_LOG {
        bigint id PK
        bigint search_request_id FK
        varchar strategy_name "검색 전략명"
        varchar primary_source "주 데이터 소스"
        varchar fallback_source "대체 데이터 소스"
        boolean fallback_used "대체 소스 사용 여부"
        int total_results "총 결과 수"
        int response_time_ms "응답 시간"
        timestamp created_at
    }

    %% Cache Management
    CACHE_ENTRY {
        varchar cache_key PK "캐시 키"
        text cache_value "캐시 값(JSON)"
        varchar cache_type "캐시 타입"
        timestamp expires_at "만료 시간"
        timestamp created_at
        timestamp updated_at
    }

    %% Circuit Breaker
    CIRCUIT_BREAKER_STATE {
        varchar service_name PK "서비스명"
        varchar state "상태(CLOSED/OPEN/HALF_OPEN)"
        int failure_count "실패 횟수"
        int success_count "성공 횟수"
        timestamp last_failure_at "마지막 실패 시간"
        timestamp state_changed_at "상태 변경 시간"
        timestamp updated_at
    }

    %% Category & Location (Phase 3)
    LOCATION_CATEGORY {
        bigint id PK
        varchar category_name "카테고리명"
        varchar region_code "지역 코드"
        decimal latitude "위도"
        decimal longitude "경도"
        varchar h2_index "H2 색인"
        boolean is_active "활성화 여부"
        timestamp created_at
        timestamp updated_at
    }

    %% Relationships
    %% 검색 관련 핵심 관계
    SEARCH_REQUEST ||--o{ SEARCH_RESULT : "generates"
    SEARCH_REQUEST ||--|| SEARCH_RECORD : "creates"
    SEARCH_REQUEST ||--|| SEARCH_EVENT : "triggers"
    SEARCH_REQUEST ||--|| SEARCH_STRATEGY_LOG : "uses_strategy"
    
    %% 맛집 관련 관계  
    SEARCH_RESULT }o--|| RESTAURANT : "references"
    
    %% 키워드 관련 관계
    SEARCH_RECORD }o--|| KEYWORD : "uses"
    KEYWORD ||--o{ KEYWORD_COUNT : "counted_by"
    KEYWORD ||--o{ POPULAR_KEYWORD : "featured_in"
    
    %% 지역 카테고리 관계
    LOCATION_CATEGORY ||--o{ KEYWORD_COUNT : "categorizes"
    LOCATION_CATEGORY ||--o{ POPULAR_KEYWORD : "categorizes" 
    LOCATION_CATEGORY ||--o{ SEARCH_RECORD : "categorizes"
    
    %% 외부 API 관계
    EXTERNAL_API_CONFIG ||--o{ API_CALL_LOG : "logs"