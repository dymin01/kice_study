import java.io.BufferedReader;  // 파일을 효율적으로 읽기 위한 클래스
import java.io.FileReader;      // 파일을 읽기 위한 클래스
import java.io.IOException;     // 입출력 예외 처리 클래스
import java.util.ArrayList;     // 동적 배열 자료구조
import java.util.HashMap;       // 키-값 쌍을 저장하는 해시맵 자료구조
import java.util.HashSet;       // 중복 없는 집합 자료구조
import java.util.List;          // 리스트 인터페이스
import java.util.Map;           // 맵 인터페이스
import java.util.Scanner;       // 입력을 처리하기 위한 스캐너 클래스

/**
 * SP_TEST 클래스는 사전과 불용어를 이용해
 * 입력 문장을 벡터로 변환하는 프로그램입니다.
 */
public class SP_TEST {
    // 단어-벡터 매핑을 저장하는 사전
    private static final Map<String, String> dictionary = new HashMap<>();
    // 불용어 집합 (벡터 변환 시 제외할 단어들)
    private static final Set<String> stopwords = new HashSet<>();

    public static void main(String[] args) throws Exception {
        // 사전 파일을 읽어 dictionary에 단어-벡터 매핑을 로드
        loadDictionary("DICTIONARY.TXT");
        // 불용어 파일을 읽어 stopwords 집합에 저장
        loadStopwords("STOPWORD.TXT");

        // 사용자 입력을 받기 위한 Scanner 객체 생성
        Scanner scanner = new Scanner(System.in);

        // 무한 루프를 돌면서 입력 문장을 계속 처리
        while (true) {
            // 한 줄 입력 받기
            String line = scanner.nextLine();
            // 입력 문장을 공백 기준으로 토큰화
            String[] tokens = line.trim().split("\\s+");
            // 변환된 벡터들을 저장할 리스트
            List<String> vectors = new ArrayList<>();

            // 각 토큰에 대해 벡터 변환 수행
            for (String token : tokens) {
                // 소문자로 변환하여 사전에서 검색
                String key = token.toLowerCase();
                String vector = dictionary.get(key);
                // 벡터가 존재하고, 불용어 집합에 포함되어 있지 않은 경우에만 추가
                if (vector != null && !stopwords.contains(vector)) {
                    vectors.add(vector);
                }
            }
            // 변환된 벡터들을 공백으로 연결하여 출력
            System.out.println(String.join(" ", vectors));
        }
    }

    // 사전 파일을 읽어서 dictionary에 단어-벡터 쌍을 저장하는 메서드
    private static void loadDictionary(String path) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            // 파일의 각 줄을 읽으며 처리
            while ((line = br.readLine()) != null) {
                // '#'을 기준으로 단어와 벡터를 분리
                String[] parts = line.split("#");
                if (parts.length == 2) {
                    dictionary.put(parts[0], parts[1]);
                }
            }
        }
    }

    // 불용어 파일을 읽어서 stopwords 집합에 저장하는 메서드
    private static void loadStopwords(String path) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            // 파일의 각 줄을 읽으며 처리
            while ((line = br.readLine()) != null) {
                stopwords.add(line.trim());
            }
        }
    }
}