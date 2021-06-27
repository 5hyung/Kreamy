package com.mvc.kreamy;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.mvc.dao.UserDAO;
import com.mvc.dto.UserDTO;
import com.mvc.util.GenerateCertPwd;

@Controller("login.controller")
public class LoginController {

	/* NaverLoginBO */
	private NaverLoginBO naverLoginBO;
	private String apiResult = null;
	
	private boolean flag = false;

	@Autowired
	private void setNaverLoginBO(NaverLoginBO naverLoginBO) {
		this.naverLoginBO = naverLoginBO;
	}

	@Autowired
	UserDAO dao;

	@Autowired
	GenerateCertPwd generateCertPwd;
	
	@RequestMapping(value = "/login", method = { RequestMethod.GET })
	public String login(Model model, HttpSession session, HttpServletRequest request) throws Exception {

		/* ���̹����̵�� ���� URL�� �����ϱ� ���Ͽ� naverLoginBOŬ������ getAuthorizationUrl�޼ҵ� ȣ�� */
		String naverAuthUrl = naverLoginBO.getAuthorizationUrl(session);

		// https://nid.naver.com/oauth2.0/authorize?response_type=code&client_id=sE***************&
		// redirect_uri=http%3A%2F%2F211.63.89.90%3A8090%2Flogin_project%2Fcallback&state=e68c269c-5ba9-4c31-85da-54c16c658125
		System.out.println("���̹�:" + naverAuthUrl);
		// ���̹�
		model.addAttribute("url", naverAuthUrl);

		return "login/login";
	}

	@RequestMapping(value = "/login_ok", method = { RequestMethod.POST, RequestMethod.GET })
	public String login_ok(UserDTO dto, HttpServletRequest request) throws Exception {

		dto = dao.getReadData(dto.getEmail(), dto.getPwd());

		if (dto == null) {
			return "redirect:login";
		}

		return "login/login_ok";
	}

	// ���̹� �α��� ������ callbackȣ�� �޼ҵ�
	@RequestMapping(value = "/callback", method = { RequestMethod.GET, RequestMethod.POST })
	public String callback(Model model, @RequestParam String code, @RequestParam String state, HttpSession session)
			throws Exception {

		System.out.println("����� callback");
		OAuth2AccessToken oauthToken;
		oauthToken = naverLoginBO.getAccessToken(session, code, state);

		// 1. �α��� ����� ������ �о�´�.
		apiResult = naverLoginBO.getUserProfile(oauthToken); // String������ json������

		/*
		 * apiResult json ���� {"resultcode":"00", "message":"success",
		 * "response":{"id":"33666449","nickname":"shinn****","age":"20-29","gender":"M"
		 * ,"email":"sh@naver.com","name":"\uc2e0\ubc94\ud638"}}
		 */

		// 2. String������ apiResult�� json���·� �ٲ�
		JSONParser parser = new JSONParser();
		Object obj = parser.parse(apiResult);
		JSONObject jsonObj = (JSONObject) obj;

		// 3. ������ �Ľ�
		// Top���� �ܰ� _response �Ľ�
		JSONObject response_obj = (JSONObject) jsonObj.get("response");

		// response�� nickname�� �Ľ�
		String nickname = (String) response_obj.get("nickname");
		System.out.println(nickname);
		
		String email = (String) response_obj.get("email");
		System.out.println(email);
		
		UserDTO dto = dao.getEmail(email);

		// 4.�Ľ� �г��� �������� ����
		session.setAttribute("sessionId", nickname); // ���� ����
		session.setAttribute("naverId", email);
		model.addAttribute("result", apiResult);
		
		if(dto==null) {
			flag = true;
			return "login/join";
		}
		
		return "login/login_ok";
	}

	// �α׾ƿ�
	@RequestMapping(value = "/logout", method = { RequestMethod.GET, RequestMethod.POST })
	public String logout(HttpSession session) throws IOException {

		System.out.println("����� logout");
		session.invalidate();

		return "redirect:login";
	}

	@RequestMapping(value = "/join", method = { RequestMethod.GET, RequestMethod.POST })
	public String signup(HttpServletRequest request, HttpSession session) throws Exception {

		System.out.println("hello");
		System.out.println(flag);
		
		if(flag==true) {
			String email = (String) session.getAttribute("naverId");
			System.out.println(email);
			request.setAttribute("email", email);
		}
		
		List<String> elists = dao.getReadEmail();
		List<String> plists = dao.getReadPhone();
		
		session.invalidate();
		
		request.setAttribute("elists", elists);
		request.setAttribute("plists", plists);

		return "login/join";
	}

	@RequestMapping(value = "/join_ok", method = { RequestMethod.GET, RequestMethod.POST })
	public String signup_ok(UserDTO dto, HttpServletRequest request) throws Exception {

		int maxNum = dao.getMaxNum();
		String email = dto.getEmail();

		int index = email.indexOf("@");
		String id = email.substring(0, index);

		System.out.println(dto.getShoesSize());

		
		dto.setUserNum(maxNum + 1);
		dto.setId(id);
	
		dao.insertData(dto);

		return "redirect:login";
	}

	@RequestMapping(value = "/find_email", method = { RequestMethod.GET, RequestMethod.POST })
	public String emailFind(HttpServletRequest request) throws Exception {

		List<String> plists = dao.getReadPhone();

		request.setAttribute("plists", plists);

		return "login/find_email";
	}

	@RequestMapping(value = "/find_email_ok", method = { RequestMethod.GET, RequestMethod.POST })
	public String emailFindOk(String phone, HttpServletRequest request) throws Exception {

		String email = dao.findEmail(phone);

		int endIndex = email.indexOf("@");
		
		String idArea = email.substring(0,1);
		String emailArea = email.substring(endIndex);
		String masking = email.substring(1, endIndex);
		
		for(int i=0; i<masking.length(); i++) {
			idArea += '*';
		}
		
		email = idArea + emailArea;
		
		request.setAttribute("email", email);
		
		return "login/find_email_ok";
	}
	
	@RequestMapping(value = "/find_password", method = { RequestMethod.GET, RequestMethod.POST })
	public String passwordFind(HttpServletRequest request) throws Exception {

		List<String> plists = dao.getReadPhone();
		List<String> elists = dao.getReadEmail();

		request.setAttribute("plists", plists);
		request.setAttribute("elists", elists);
		
		return "login/find_password";
	}
	
	@RequestMapping(value = "/find_password_ok", method = { RequestMethod.GET, RequestMethod.POST })
	public String passwordFindOk(String email) throws Exception {
		
		System.out.println(email);
		String newPwd = "";
		
		while(true) {
			newPwd = generateCertPwd.excuteGenerate();
			if(newPwd!=null) {
				break;
			}
		}
			
		System.out.println(newPwd);
		dao.updatePwd(email, newPwd);
		
		//�̸��Ϸ� �����°� �߰�,,, �غ�����~!
		
		return "login/find_password_ok";
	}
	
	
}
