import java.io.BufferedReader; // 파일 읽기용 BufferedReader
import java.io.FileReader; // 파일 읽기용 FileReader
import java.io.IOException; // 입출력 예외 처리
import java.io.InputStreamReader; // 입력 스트림 읽기용
import java.io.Reader; // Reader 인터페이스
import java.util.ArrayList; // 동적 배열 구현용 ArrayList
import java.util.HashMap; // 키-값 쌍 저장용 HashMap
import java.util.HashSet; // 중복 없는 집합용 HashSet
import java.util.List; // 리스트 인터페이스
import java.util.Map; // 맵 인터페이스
import java.util.Set; // 집합 인터페이스

import javax.servlet.http.HttpServlet; // 서블릿 기본 클래스
import javax.servlet.http.HttpServletRequest; // HTTP 요청 처리용
import javax.servlet.http.HttpServletResponse; // HTTP 응답 처리용

import org.eclipse.jetty.client.HttpClient; // Jetty HTTP 클라이언트
import org.eclipse.jetty.client.api.ContentResponse; // HTTP 응답 객체
import org.eclipse.jetty.client.util.StringContentProvider; // 문자열을 HTTP 바디로 제공
import org.eclipse.jetty.http.HttpHeader; // HTTP 헤더 상수
import org.eclipse.jetty.server.Server; // Jetty 서버
import org.eclipse.jetty.servlet.ServletHandler; // 서블릿 핸들러

import com.google.gson.Gson; // JSON 직렬화/역직렬화 라이브러리
import com.google.gson.JsonArray; // JSON 배열 처리
import com.google.gson.JsonElement; // JSON 요소
import com.google.gson.JsonObject; // JSON 객체

// 메인 실행 클래스: 서버 구동 및 모델, 사전, 불용어 데이터 로드 담당
public class SP_TEST {
	// 단어-벡터 매핑 사전 저장용
	private static final Map<String, String> dictionary = new HashMap<>();
	// 불용어 집합 저장용
	private static final Set<String> stopwords = new HashSet<>();
	// 모델 정보 리스트 저장용
	private static final List<ModelInfo> models = new ArrayList<>();

	public static void main(String[] args) throws Exception {
		// 사전, 불용어, 모델 정보 파일 로드
		loadDictionary("DICTIONARY.TXT");
		loadStopwords("STOPWORD.TXT");
		loadModels("MODELS.JSON");

		// Jetty 서버 8080 포트로 구동 및 서블릿 핸들러 등록
		Server server = new Server(8080); // 포트 8080에서 Jetty 서버 생성
		ServletHandler handler = new ServletHandler(); // 서블릿 매핑을 위한 핸들러 준비
		handler.addServletWithMapping(MainServlet.class, "/"); // MainServlet을 루트 경로에 등록
		server.setHandler(handler); // 서버에 핸들러 설정
		server.start(); // 서버 시작 및 요청 대기 시작
		server.join(); // 서버가 종료될 때까지 대기
	}

	// 모델 정보 구조 클래스: 모델명, URL, 클래스 목록 포함
	public static class ModelInfo {
		String modelname; // 모델 이름
		String url; // 모델 서버 URL
		List<ClassInfo> classes; // 클래스 목록
	}

	// 클래스 정보 구조 클래스: 코드와 값(레이블) 저장
	public static class ClassInfo {
		String code; // 클래스 코드
		String value; // 클래스 값(레이블)
	}

	// 메인 서블릿: 클라이언트 요청 처리 및 모델 예측 결과 반환 역할
	public static class MainServlet extends HttpServlet {
		@Override
		protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
			// 요청 JSON 파싱
			Gson gson = new Gson();
			JsonObject requestJson = gson.fromJson(new InputStreamReader(req.getInputStream()), JsonObject.class);
			String modelName = requestJson.get("modelname").getAsString();
			JsonArray queries = requestJson.getAsJsonArray("queries");

			// 요청한 모델 정보 조회
			ModelInfo model = models.stream().filter(m -> m.modelname.equals(modelName)).findFirst().orElse(null);
			if (model == null) {
				// 모델이 없으면 에러 응답 반환
				resp.setStatus(400);
				resp.getWriter().write("{\"error\":\"Model not found\"}");
				return;
			}

			// 각 쿼리에 대해 전처리 및 모델 요청 수행 후 결과 수집
			List<String> results = new ArrayList<>();
			for (JsonElement queryElem : queries) {
				String query = queryElem.getAsString();
				// 입력 문장 전처리 (토큰화, 임베딩, 불용어 제거)
				String processed = preprocess(query);
				// 전처리된 결과를 모델 서버에 요청하여 예측 코드 획득
				String code = requestModel(model.url, processed);
				// 예측 코드에 대응하는 클래스 값 찾기, 없으면 "unknown"
				String value = model.classes.stream().filter(c -> c.code.equals(code)).map(c -> c.value).findFirst()
						.orElse("unknown");
				results.add(value);
			}
			// 결과 JSON 생성 및 응답
			JsonObject responseJson = new JsonObject();
			JsonArray resArr = new JsonArray();
			for (String r : results)
				resArr.add(r);
			responseJson.add("results", resArr);

			resp.setContentType("application/json");
			resp.getWriter().write(gson.toJson(responseJson));
		}

		// 문장 전처리 메소드: 입력 문장을 토큰화 후 사전에서 벡터로 변환하고 불용어 제거
		private String preprocess(String sentence) {
			String[] tokens = sentence.trim().split("\\s+");
			List<String> vectors = new ArrayList<>();
			for (String token : tokens) {
				String key = token.toLowerCase();
				String vector = dictionary.get(key);
				// 벡터가 존재하고 불용어에 해당하지 않으면 결과에 추가
				if (vector != null && !stopwords.contains(vector)) {
					vectors.add(vector);
				}
			}
			// 벡터들을 공백으로 연결하여 반환
			return String.join(" ", vectors);
		}

		// 모델 서버에 HTTP POST 요청을 보내 전처리된 쿼리로부터 예측 결과를 받아옴
		private String requestModel(String url, String processed) {
			Gson gson = new Gson(); // JSON 처리용 Gson 인스턴스
			HttpClient httpClient = new HttpClient(); // Jetty HttpClient 생성
			try {
				httpClient.start(); // HttpClient 시작
				// JSON 형식의 요청 바디 생성
				String json = String.format("{\"query\":\"%s\"}", processed);

				// POST 요청 생성 및 전송
				ContentResponse response = httpClient.POST(url).header(HttpHeader.CONTENT_TYPE, "application/json") // Content-Type 지정
						.content(new StringContentProvider(json), "application/json") // JSON 바디 설정
						.send(); // 동기 전송

				// 응답 바디 파싱 후 결과 추출
				String responseBody = response.getContentAsString();
				JsonObject res = gson.fromJson(responseBody, JsonObject.class);
				httpClient.stop(); // HttpClient 종료 (실전에서는 재사용 권장)
				return res.get("result").getAsString(); // 결과 문자열 반환
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null; // 예외 발생 시 null 반환
		}
	}

	// 사전 파일을 읽어 단어와 벡터 매핑 정보를 dictionary에 저장하는 메소드
    private static void loadDictionary(String path) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("#");
                if (parts.length == 2) {
                    dictionary.put(parts[0], parts[1]);
                }
            }
        }
    }

	// 불용어 파일을 읽어 한 줄씩 stopwords 집합에 추가하는 메소드
    private static void loadStopwords(String path) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                stopwords.add(line.trim());
            }
        }
    }

	// JSON 형식의 모델 정보 파일을 읽어 모델 리스트에 파싱하여 저장하는 메소드
	private static void loadModels(String path) throws IOException {

		Gson gson = new Gson();
		try (Reader reader = new FileReader(path)) {
			JsonObject obj = gson.fromJson(reader, JsonObject.class);
			JsonArray arr = obj.getAsJsonArray("models");
			for (JsonElement e : arr) {
				models.add(gson.fromJson(e, ModelInfo.class));
			}
		}
	}
}