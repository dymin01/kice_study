import java.io.BufferedReader; // 파일 읽기용 BufferedReader 임포트
import java.io.FileReader; // 파일 읽기용 FileReader 임포트
import java.io.IOException; // 입출력 예외 처리용 IOException 임포트
import java.util.ArrayList; // 동적 배열 사용을 위한 ArrayList 임포트
import java.util.HashMap; // 해시맵 사용을 위한 HashMap 임포트
import java.util.List; // 리스트 인터페이스 임포트
import java.util.Map; // 맵 인터페이스 임포트
import java.util.Scanner; // 사용자 입력을 받기 위한 Scanner 임포트

// 단어 사전을 불러와 입력 문장을 벡터로 변환하는 프로그램 역할을 하는 클래스
public class SP_TEST {
    // 단어-벡터 매핑 저장용 Map
    private static final Map<String, String> dictionary = new HashMap<>();

    public static void main(String[] args) throws Exception {
        // 먼저 dictionary 파일을 로드
        loadDictionary("DICTIONARY.TXT");
        // Scanner로 사용자 입력을 받음
        Scanner scanner = new Scanner(System.in);
        // while(true) 무한 루프로 사용자 입력을 반복 처리
        while (true) {
            // 사용자로부터 한 줄 입력 받음
            String line = scanner.nextLine();
            // 입력된 문장을 공백 기준으로 토큰화하고 리스트 생성
            String[] tokens = line.trim().split("\\s+");
            List<String> L = new ArrayList<>();
            // 각 토큰을 소문자로 변환하여 dictionary에서 key 확인
            for (String token : tokens) {
                String key = token.toLowerCase();
                // 사전에 존재하면 vectors 리스트에 해당 벡터 추가
                if (dictionary.containsKey(key)) {
                    vectors.add(dictionary.get(key));
                }
            }
            // 변환된 벡터들을 공백으로 연결하여 출력
            System.out.println(String.join(" ", vectors));
        }

    }

    // 단어 사전 파일을 읽어 한 줄씩 분리하여 dictionary에 저장하는 메서드
    private static void loadDictionary(String path) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            // 파일의 각 줄을 읽으면서 '#' 기준으로 단어와 벡터 분리 후 저장
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("#");
                if (parts.length == 2) {
                    dictionary.put(parts[0], parts[1]);
                }
            }
        }
    }
}