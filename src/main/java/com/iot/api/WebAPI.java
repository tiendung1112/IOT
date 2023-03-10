package com.iot.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.iot.authentication.JwtTokenProvider;
import com.iot.authentication.MyUser;
import com.iot.dto.DeviceDto;
import com.iot.dto.RoleDto;
import com.iot.dto.SensorAllDto;
import com.iot.dto.SensorDataDto;
import com.iot.dto.SensorDto;
import com.iot.dto.UserDto;
import com.iot.payloads.JwtAuthRequest;
import com.iot.payloads.JwtAuthResponse;
import com.iot.service.IDeviceService;
import com.iot.service.IRoleService;
import com.iot.service.ISensorDataService;
import com.iot.service.ISensorService;
import com.iot.service.IUserService;

@RestController(value = "homeApiControllerOfWeb")
//@CrossOrigin
@CrossOrigin(origins = "*")
public class WebAPI {
	@Autowired
	private IRoleService roleService;
	@Autowired
	private IUserService userService;
	@Autowired
	private IDeviceService deviceService;
	@Autowired
	private ISensorDataService sensorDataService;
	@Autowired
	private ISensorService sensorService;

	@Autowired
	AuthenticationManager authenticationManager;
	@Autowired
	private JwtTokenProvider tokenProvider;

	/*
	 * ????ng nh???p
	 */
	@PostMapping("/api/auth/signin")
	public JwtAuthResponse signin(@RequestBody UserDto loginRequest) {
		Authentication authentication = null;
		try {
			authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
			// N???u kh??ng x???y ra exception t???c l?? th??ng tin h???p l???
			// Set th??ng tin authentication v??o Security Context
			SecurityContextHolder.getContext().setAuthentication(authentication);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		MyUser userPrincipal = (MyUser) authentication.getPrincipal();
		// Tr??? v??? jwt cho ng?????i d??ng.
		String jwt = tokenProvider.generateJwtTokenUsername(authentication);
		return new JwtAuthResponse(userPrincipal, jwt);
	}

	// th??m m???i user
	@PostMapping(value = "/api/auth")
	private UserDto register(@RequestBody UserDto user) {
		UserDto result = userService.save(user);
		return result;
	}

	// th??ng tin user
	@GetMapping("/api/auth/{id}")
	public UserDto getProfile(@PathVariable("id") Long id) {
		return userService.findById(id);
	}

	// Generate TokenUser
	@PostMapping("/api/auth/token")
	public String getTokenUser(@RequestBody UserDto dto) {
		Authentication authentication = null;
		try {
			authentication = authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));
			SecurityContextHolder.getContext().setAuthentication(authentication);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		// Tr??? v??? jwt cho ng?????i d??ng.
		String jwt = "token: " + tokenProvider.generateJwtTokenUsername(authentication);

		return jwt;
	}

	/*
	 * L???y danh s??ch device ???ng v???i user d???a v??o jwt c???a user
	 */
	@GetMapping("/api/device/list")
	public List<DeviceDto> getListDeviceByUser(HttpServletRequest request) {
		List<DeviceDto> result = new ArrayList<DeviceDto>();
		String user_token = "";
		String token = "";
		String bearerToken = request.getHeader("Authorization");
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			token = bearerToken.substring(7);
			if (tokenProvider.validateJwtToken(token)) {
				user_token = tokenProvider.getUserNameFromJwtToken(token);
				result = deviceService.getListDeviceByUser(user_token);
			}
		}

		return result;
	}

	/*
	 * Th??m m???i thi???t b??? user t??? th??m d???a v??o jwt c???a user
	 */
	@PostMapping("/api/device")
	public JwtAuthRequest saveDevice(@RequestBody DeviceDto device, HttpServletRequest request) {
		String user_token = "";
		JwtAuthRequest result = new JwtAuthRequest();
		String bearerToken = request.getHeader("Authorization");
		// Ki???m tra xem header Authorization c?? ch???a th??ng tin jwt kh??ng
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			user_token = bearerToken.substring(7);
			if (tokenProvider.validateJwtToken(user_token)) {
				UserDto user = userService.getUserWithUsername(tokenProvider.getUserNameFromJwtToken(user_token));
				device.setUserDto(user);
				device = deviceService.save(device);
				result.setDeviceDto(device);
				result.setDeviceId(device.getId());
				result.setToken(user_token);
			}
		}

		/*
		 * result ???? c?? ????? c??? sensor list nh??
		 */
		return result;
	}

	/*
	 * C???p nh???t thi???t b??? d???a v??o jwt c???a user
	 */
	@PutMapping("/api/device")
	public DeviceDto upDateDevice(@RequestBody DeviceDto device, HttpServletRequest request) {
		DeviceDto result = null;
		String user_token = "";
		String bearerToken = request.getHeader("Authorization");
		// Ki???m tra xem header Authorization c?? ch???a th??ng tin jwt kh??ng
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			user_token = bearerToken.substring(7);
			if (tokenProvider.validateJwtToken(user_token)) {
				UserDto user = userService.getUserWithUsername(tokenProvider.getUserNameFromJwtToken(user_token));
				device.setUserDto(user);
				result = deviceService.save(device);
				result.setUserDto(user);
			}
		}
		// result ???? c?? ????? c??? sensor list nh??
		return result;
	}

	/*
	 * L???y t???t c??? th??ng tin c???a thi???t b??? d???a v??o jwt user
	 * 
	 */
	@GetMapping("/api/device/{id}")
	public DeviceDto getInfoDevice(@PathVariable("id") Long id, HttpServletRequest request) {
		DeviceDto result = null;
		String user_token = "";
		String bearerToken = request.getHeader("Authorization");
		// Ki???m tra xem header Authorization c?? ch???a th??ng tin jwt kh??ng
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			user_token = bearerToken.substring(7);
			if (tokenProvider.validateJwtToken(user_token)) {
				UserDto user = userService.getUserWithUsername(tokenProvider.getUserNameFromJwtToken(user_token));
				result = deviceService.getInfoDevice(id, user.getUsername());
				result.setUserDto(user);
			}
		}
		return result;
	}

	/*
	 * Generate token device authen(Active device) d???a v??o jwt user
	 */
	@GetMapping("/api/device/{id}/generatetoken")
	public JwtAuthRequest getGenerateAuthDevice(@PathVariable("id") Long id, HttpServletRequest request) {
		String user_token = "";
		DeviceDto deviceDto = deviceService.findById(id);
		JwtAuthRequest result = new JwtAuthRequest();
		String bearerToken = request.getHeader("Authorization");
		if (deviceDto != null && StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			user_token = bearerToken.substring(7);
			if (tokenProvider.validateJwtToken(user_token)) {
				UserDto user = userService.getUserWithUsername(tokenProvider.getUserNameFromJwtToken(user_token));
				result.setDeviceId(deviceDto.getId());

				result.setToken(tokenProvider.generateTokenAuthActiveDevice(user.getUsername(), deviceDto.getId()));
			}
		}
		return result;
	}

	/*
	 * Generate token device collect data d???a v??o jwt user
	 */
	@GetMapping("/api/device/{id}/generatetokencollect")
	public JwtAuthRequest getGenerateTokenCollect(@PathVariable("id") Long id, HttpServletRequest request) {
		String user_token = "";
		DeviceDto deviceDto = deviceService.findById(id);
		JwtAuthRequest result = new JwtAuthRequest();
		String bearerToken = request.getHeader("Authorization");
		if (deviceDto != null && StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			user_token = bearerToken.substring(7);
			if (tokenProvider.validateJwtToken(user_token)) {
				UserDto user = userService.getUserWithUsername(tokenProvider.getUserNameFromJwtToken(user_token));
				result.setDeviceId(deviceDto.getId());

				result.setToken(tokenProvider.generateTokenCollectData(user.getId(), deviceDto.getId()));
			}
		}
		return result;
	}

	/*
	 * L???y d??? li???u c???p nh???t l???n cu???i
	 */
	@GetMapping("/api/device/{id}/lastdata")
	public List<SensorDataDto> getLastDataSensor(@PathVariable("id") Long id) {
		List<SensorDataDto> result = sensorDataService.findAllDataLastSensorId(id);
		return result;
	}

	/*
	 * L???y t???t c??? d??? li???u sensor c?? status=1 li??n quan ?????n device
	 */
	@GetMapping("/api/device/{id}/alldata")
	public List<SensorDto> getAllDataSensor(@PathVariable("id") Long id) {
		List<SensorDto> result = sensorService.getAllData(id, "", "");
		return result;
	}

	/*
	 * L???y list sensor ??ang c?? status=1 li??n quan ?????n device
	 */
	@GetMapping("/api/device/{id}/listsensor")
	public List<SensorDto> getListSensorOfDevice(@PathVariable("id") Long id) {
		List<SensorDto> result = new ArrayList<SensorDto>(deviceService.getListSensor(id).getSensorList());
		return result;
	}

	/*
	 * Ph???n li??n quan ?????n admin
	 */

	/*
	 * L???y danh s??ch user
	 */
	@GetMapping("/api/auth/list")
	public List<UserDto> getAllUser(HttpServletRequest request) {
		List<UserDto> result = new ArrayList<UserDto>();
		String user_token = "";
		String bearerToken = request.getHeader("Authorization");
		// Ki???m tra xem header Authorization c?? ch???a th??ng tin jwt kh??ng
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			user_token = bearerToken.substring(7);
			if (tokenProvider.validateJwtToken(user_token)) {
				UserDto admin = userService.getUserWithUsername(tokenProvider.getUserNameFromJwtToken(user_token));
				if (admin.getRoleDto().getCode().equals("ADMIN")) {
					result = userService.findAll();
				}
			}
		}
		return result;
	}

	/*
	 * L???y t???t c??? danh s??ch device
	 */
	@GetMapping("/api/admin/device/list")
	public List<DeviceDto> getListDeviceByAdmin(HttpServletRequest request) {
		List<DeviceDto> result = new ArrayList<DeviceDto>();
		String user_token = "";
		String token = "";
		String bearerToken = request.getHeader("Authorization");
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			token = bearerToken.substring(7);
			if (tokenProvider.validateJwtToken(token)) {
				user_token = tokenProvider.getUserNameFromJwtToken(token);

				result = deviceService.getListDeviceByAdmin(user_token);

			}
		}

		return result;
	}
	
	/*
	 * L???y danh t???ng s???n l?????ng n?????c theo th??ng
	 */
	@GetMapping("/api/admin/quantity/list")
	public List<DeviceDto> getListQuantityByAdmin(HttpServletRequest request) {
		List<DeviceDto> result = new ArrayList<DeviceDto>();
		String user_token = "";
		String token = "";
		String bearerToken = request.getHeader("Authorization");
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			token = bearerToken.substring(7);
			if (tokenProvider.validateJwtToken(token)) {
				user_token = tokenProvider.getUserNameFromJwtToken(token);

				result = deviceService.getListDeviceByAdmin(user_token);

			}
		}

		return result;
	}	

	/*
	 * Th??m m???i thi???t b??? admin th??m
	 */
	@PostMapping("/api/admin/device")
	public JwtAuthRequest saveDeviceByAdmin(@RequestBody DeviceDto device, HttpServletRequest request) {
		String user_token = "";
		JwtAuthRequest result = new JwtAuthRequest();
		String bearerToken = request.getHeader("Authorization");
		// Ki???m tra xem header Authorization c?? ch???a th??ng tin jwt kh??ng
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			user_token = bearerToken.substring(7);
			if (tokenProvider.validateJwtToken(user_token)) {
				UserDto admin = userService.getUserWithUsername(tokenProvider.getUserNameFromJwtToken(user_token));
				if (admin.getRoleDto().getCode().equals("ADMIN")) {
					UserDto user = userService.getUserWithUsername(device.getUserDto().getUsername());
					device.setUserDto(user);
					device = deviceService.save(device);
					result.setDeviceDto(device);
					result.setDeviceId(device.getId());
					result.setToken(device.getToken_auth());
				}
			}
		}
		return result;
	}

	/*
	 * L???y th??ng danh s??ch device ???ng v???i 1 user d???a v??o jwt admin return:
	 * List(th??ng tin device v?? sensor ???ng v???i device ???? ko tr??? v??? sensor datalist)
	 */
	@GetMapping("/api/admin/{username}/device/list")
	public List<DeviceDto> getListDeviceUserByAdmin(@PathVariable("username") String username,
			HttpServletRequest request) {
		List<DeviceDto> result = new ArrayList<DeviceDto>();
		@SuppressWarnings("unused")
		String user_token = "";
		String token = "";
		String bearerToken = request.getHeader("Authorization");
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			token = bearerToken.substring(7);
			if (tokenProvider.validateJwtToken(token)) {
				user_token = tokenProvider.getUserNameFromJwtToken(token);
				result = deviceService.getListDeviceByUser(username);
			}
		}

		return result;
	}

	/*
	 * L???y t???t c??? th??ng tin c???a thi???t b??? d???a v??o jwt admin return: th??ng tin device
	 * v?? sensor ???ng v???i device v?? list sensordata ???ng v???i sensor
	 */
	@GetMapping("/api/admin/{username}/device/{id}")
	public DeviceDto getInfoDeviceUserByAdmin(@PathVariable("id") Long id, @PathVariable("username") String username,
			HttpServletRequest request) {
		DeviceDto result = null;
		String user_token = "";
		String bearerToken = request.getHeader("Authorization");
		// Ki???m tra xem header Authorization c?? ch???a th??ng tin jwt kh??ng
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			user_token = bearerToken.substring(7);
			if (tokenProvider.validateJwtToken(user_token)) {
				UserDto admin = userService.getUserWithUsername(tokenProvider.getUserNameFromJwtToken(user_token));
				if (admin != null && admin.getRoleDto().getCode().equals("ADMIN")) {
					UserDto user = userService.getUserWithUsername(username);
					result = deviceService.getInfoDevice(id, user.getUsername());
					result.setUserDto(user);
				}
			}
		}
		return result;
	}

	/*
	 * C???p nh???t thi???t b??? d???a v??o jwt c???a admin
	 */
	@PutMapping("/api/admin/{username}/device")
	public DeviceDto upDateDevice(@RequestBody DeviceDto device, @PathVariable("username") String username,
			HttpServletRequest request) {
		DeviceDto result = null;
		String user_token = "";
		String bearerToken = request.getHeader("Authorization");
		// Ki???m tra xem header Authorization c?? ch???a th??ng tin jwt kh??ng
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			user_token = bearerToken.substring(7);
			if (tokenProvider.validateJwtToken(user_token)) {
				UserDto admin = userService.getUserWithUsername(tokenProvider.getUserNameFromJwtToken(user_token));
				if (admin != null && admin.getRoleDto().getCode().equals("ADMIN")) {
					UserDto user = userService.getUserWithUsername(username);
					device.setUserDto(user);
					result = deviceService.save(device);
					result.setUserDto(user);
				}
			}
		}
		// result ???? c?? ????? c??? sensor list nh??
		return result;
	}

	/*
	 * Th??m th??nh vi??n m???i
	 */
	@PostMapping(value = "/api/admin/auth/signup")
	private JwtAuthResponse signUp(@RequestBody UserDto user, HttpServletRequest request) {
		JwtAuthResponse response = new JwtAuthResponse();
		String bearerToken = request.getHeader("Authorization");
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			String adminToken = bearerToken.substring(7);
			if (tokenProvider.validateJwtToken(adminToken)) {
				UserDto admin = userService.getUserWithUsername(tokenProvider.getUserNameFromJwtToken(adminToken));
				if (admin != null && admin.getRoleDto().getCode().equals("ADMIN")) {
					UserDto result = userService.save(user);
					if (result != null) {
						Authentication authentication = authenticationManager.authenticate(
								new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));
						SecurityContextHolder.getContext().setAuthentication(authentication);
						String jwt = tokenProvider.generateJwtTokenUsername(authentication);
						MyUser userPrincipal = (MyUser) authentication.getPrincipal();
						response.setJwt(jwt);
						response.setUser(userPrincipal);
					}
				}
			}
		}
		return response;
	}

	// x??a user
	@DeleteMapping(value = "/api/admin/auth")
	public Boolean deleteUser(@RequestBody Long[] ids, HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization");
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			String adminToken = bearerToken.substring(7);
			if (tokenProvider.validateJwtToken(adminToken)) {
				UserDto admin = userService.getUserWithUsername(tokenProvider.getUserNameFromJwtToken(adminToken));
				if (admin != null && admin.getRoleDto().getCode().equals("ADMIN")) {
					return userService.deleteUser(ids);
				}
			}
		}
		return false;
	}

	// c???p nh???t user
	@PutMapping("/api/admin/auth")
	public UserDto updateUser(@RequestBody UserDto user, HttpServletRequest request) {
		UserDto result = null;
		String bearerToken = request.getHeader("Authorization");
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			String adminToken = bearerToken.substring(7);
			if (tokenProvider.validateJwtToken(adminToken)) {
				UserDto admin = userService.getUserWithUsername(tokenProvider.getUserNameFromJwtToken(adminToken));
				if (admin != null && admin.getRoleDto().getCode().equals("ADMIN")) {
					result = userService.save(user);
				}
			}
		}
		return result;
	}

	/*
	 * Th??m m???i role
	 */
	@PostMapping(value = "/api/admin/role")
	private String saveRole(@RequestBody RoleDto role, HttpServletRequest request) {
		RoleDto result = roleService.save(role);
		if (result != null) {
			return "Save role success";
		}
		return "Save role false";
	}

	/*
	 * L???y t???t c??? danh s??ch role
	 */
	@GetMapping("/api/admin/role/list")
	private List<RoleDto> getListRole(HttpServletRequest request) {
		List<RoleDto> result = new ArrayList<RoleDto>();
		String bearerToken = request.getHeader("Authorization");
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			String adminToken = bearerToken.substring(7);
			if (tokenProvider.validateJwtToken(adminToken)) {
				UserDto admin = userService.getUserWithUsername(tokenProvider.getUserNameFromJwtToken(adminToken));
				if (admin != null && admin.getRoleDto().getCode().equals("ADMIN")) {
					result = roleService.getListRole();
				}
			}
		}
		return result;
	}

	/*
	 * L???y t???t c??? d??? li???u sensor (theo prop= year,month,day) c?? status=1 li??n quan
	 * ?????n device prop TH1: year th?? date=2020 TH2: month: date=8 l???y theo t???t c???
	 * th??ng 8 c??n date=8-2020 th?? l???y th??ng 8 c???a 2020 TH3: day: date=10 l???y t???t c???
	 * ngay 10. date=10-8 th?? l???y ngay 10 th??ng 8 c???a t???t c??? c??c n??m. date=10-8-2020
	 * date=10--2020 l???y t???t c??? ng??y 10 c???a n??m 2020
	 * 
	 * v?? d??? url=http://localhost:8080/SpringIOT/api/device/3/alldata/day/10-8-2020
	 */
	@GetMapping("/api/device/{id}/alldata/{prop}/{date}")
	public List<SensorDto> getAllDataSensorProp(@PathVariable("id") Long id, @PathVariable("prop") String prop,
			@PathVariable("date") String date, HttpServletRequest request) {
		List<SensorDto> result = sensorService.getAllData(id, prop, date);
		return result;
	}

	/*
	 * Mu???n l???y theo th??ng truy???n v??o:
	 * http://localhost:8080/SpringIOT/api/device/3/alldatasensor/month/12-2020
	 * return list g???m c??c ng??y trong th??ng c?? d??? li???u ???ng v???i list sensor c???a
	 * device
	 * 
	 * Mu???n l???y theo n??m truy???n v??o
	 * :http://localhost:8080/SpringIOT/api/device/3/alldatasensor/year/2020 return
	 * list g???m c??c th??ng trong n??m c?? d??? li???u ???ng v???i list sensor c???a device
	 */
	@GetMapping("/api/device/{id}/alldatasensor/{prop}/{date}")
	public List<SensorAllDto> getAllDataSensorWithProp(@PathVariable("id") Long id, @PathVariable("prop") String prop,
			@PathVariable("date") String date, HttpServletRequest request) {
		return sensorService.getAllSensorData(id, prop, date);
	}

	@GetMapping("/api/device/sumdata/{prop}/{date}")
	public Float getSumDataProp(@PathVariable("prop") String prop, @PathVariable("date") String date, HttpServletRequest request) {
		Float result = 0f;
		result = sensorDataService.getSumDataProp(prop, date);
		return result;
	}
	
	@GetMapping("/api/device/alldatabymonth/{prop}/{date}")
	public List<Float> getAllDataSensorWithPropByYear(@PathVariable("prop") String prop, @PathVariable("date") String date, HttpServletRequest request) {
//		return sensorService.getAllSensorData(id, prop, date);
		List<Float> result = new ArrayList<Float>();
		
		for(int i = 1; i <= 12; i++) {
			String newDate = String.valueOf(i) + "-" + date;
			Float res = sensorDataService.getSumDataPropByMonth(prop, newDate);
			result.add(res);
		}
		
		return result;
	}
	
	@GetMapping("/api/device/alldata/{id}/{year}")
	public List<Float> getAllDataUser(@PathVariable("id") Long id, @PathVariable("year") String year) {
//		return sensorService.getAllSensorData(id, prop, date);
		List<Float> result = new ArrayList<Float>();
		
		for(int i = 1; i <= 12; i++) {
			Float res = sensorDataService.getDateUserByMonth(id, String.valueOf(i), year);
			result.add(res);
		}
		
		return result;
	}
	
	// Lay tong toan bo thiet bi trong cac nam tu nam 2000
	@GetMapping("/api/device/alldata")
	public List<Float> getAllData(){
		List<Float> result = new ArrayList<Float>();
		
		for(int i = 2000; i <= 2022; i++) {
			Float res = 0f;
			res = sensorDataService.getSumDataProp("year", String.valueOf(i));
			result.add(res);
		}
		
		return result;
	}
}
