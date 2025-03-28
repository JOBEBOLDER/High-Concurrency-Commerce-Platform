package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

  @Override
  public Result sendCode(String phone, HttpSession session) {
    //check the phone
    if (RegexUtils.isPhoneInvalid(phone)) {
      return Result.fail("your ohone pattern is not correct! please check again");
    }

    // if not match ,return the error msg
    String code = RandomUtil.randomNumbers(6);

    // if match ,create the match code
    session.setAttribute("code",code);

    //send the verification code
    log.debug("you successfully send the verification code ,your code is:{}");


    return Result.ok();
  }

  @Override
  public Result login(LoginFormDTO loginForm, HttpSession session) {
    //user need to check the phone validation
    String phone = loginForm.getPhone();
    if (RegexUtils.isPhoneInvalid(phone)) {
      return Result.fail("your ohone pattern is not correct! please check again");
    }

    //user also need to check the vrification code
    Object cacheCode = session.getAttribute( "code");
    // if is not matching ,publish error
    String code = loginForm.getCode();
    if (cacheCode == null || !cacheCode.toString().equals(code)) {
      return Result.fail("your code is not correct! please check again");
    }
    //if match ,find user base on the phone number

    User user = query().eq("phone", phone).one();
    // find user if exist
    // if not exist ,create new user and store
    if (user == null) {
      user = createuserWithPhone(phone);
    }


    //store the user info to session
    session.setAttribute("user",user);
    return Result.ok();
  }

  private User createuserWithPhone(String phone) {
    //create the user
    User user = new User();
    user.setPhone(phone);
    user.setNickName("user_" + RandomUtil.randomNumbers(10));
    //store the user
    save(user);
    return user;
  }
}
