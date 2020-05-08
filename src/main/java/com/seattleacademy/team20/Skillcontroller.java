package com.seattleacademy.team20;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

@Controller
public class Skillcontroller {

	private static final Logger logger = LoggerFactory.getLogger(Skillcontroller.class);
	@Autowired
	private JdbcTemplate jdbcTemplate;

	//	MySQLとの接続するため
	/**
	 * Simply selects the home view to render by returning its name.
	 * @throws IOException
	 */
	@RequestMapping(value = "/skillUpload", method = RequestMethod.GET)
	public String skillUpload(Locale locale, Model model) throws IOException {
		logger.info("Welcome Skillupload! The client locale is {}.", locale);
		initialize();
		List<Skill> skills = selectSkills();
		uploadSkill(skills);

		return "skillUpload";
	}

	//Listの宣言

	public List<Skill> selectSkills() {
		final String sql = "select * from skills";
		return jdbcTemplate.query(sql, new RowMapper<Skill>() {
			public Skill mapRow(ResultSet rs, int rowNum) throws SQLException {
				return new Skill(rs.getString("category"),
						rs.getString("name"), rs.getInt("score"));
			}
		});
	}

	//管理者権限？？
	private FirebaseApp app;

	//Fetch the service account key JSON file contents
	//	SDKの初期化
	public void initialize() throws IOException {
		FileInputStream refreshToken = new FileInputStream(
				"/Users/eisuke-gonza/Downloads/deploy-portfolio-2089b-firebase-adminsdk-z4smk-e196969c86.json");
		FirebaseOptions options = new FirebaseOptions.Builder()
				.setCredentials(GoogleCredentials.fromStream(refreshToken))
				.setDatabaseUrl("https://deploy-portfolio-2089b.firebaseio.com/")
				.build();
		app = FirebaseApp.initializeApp(options, "other");
	}

	public void uploadSkill(List<Skill> skills) {
		final FirebaseDatabase database = FirebaseDatabase.getInstance(app);
		DatabaseReference ref = database.getReference("skillCategories");
		//		データの取得（MySQLから）
		//		データを取得してから形成する
		//		databaseにアップロードする
		// JSPに渡すデータを設定する
		List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
		Map<String, Object> map;
		Map<String, List<Skill>> skillMap = skills.stream().collect(Collectors.groupingBy(Skill::getCategory));
		for (Map.Entry<String, List<Skill>> entry : skillMap.entrySet()) {
			//		    System.out.println(entry.getKey());
			//		    System.out.println(entry.getValue());
			map = new HashMap<>();
			map.put("category", entry.getKey());
			map.put("skills", entry.getValue());

			dataList.add(map);
		}
		//      リアルタイムデータベース更新
		ref.setValue(dataList, new DatabaseReference.CompletionListener() {
			@Override
			public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
				if (databaseError != null) {
					System.out.println("Data could be saved" + databaseError.getMessage());
				} else {
					System.out.println("Data save successfully.");
				}
			}
		});
	}

	//                 ↓メインクラス名
	public class Skill {
		private String category;
		private String name;
		private int score;

		public Skill(String category, String name, int score) {
			this.category = category;
			this.name = name;
			this.score = score;
		}

		public String getCategory() {
			return category;
		}

		public String getName() {
			return name;
		}

		public int getScore() {
			return score;
		}
		//	    Map<String, List<Skill>> grpByType = Skill.stream().collect(
		//                Collectors.groupingBy(Skill::getSkillCategory));
	}
}
