package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
}
