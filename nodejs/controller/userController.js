const models = require("../models");
const Sequelize = require('sequelize');
const { sequelize } = require('../models')
const Op = Sequelize.Op;
const { sign, refresh, verify } = require('../util/jwt-util');
const bcrypt = require("bcrypt")
const authJwt = require('../util/authJWT');
const jwt = require('jsonwebtoken');
const { TableHints } = require("sequelize");
const swaggerAutogen = require("swagger-autogen");
const registerKwanJang = require("../controller/kwanjangController");
const secret = "Hello";
const { redisCli } = require("../util/redis-util");


module.exports = {
    //회원가입
    register: async (req, res, next) => {
        // #swagger.description = "유저를을 등록합니다."
        // #swagger.tags = ["register"]
        // #swagger.summary = "유저 등록"
        try{
            let {username, password, role, phone_number, last_name, first_name, email } = req.body;
            
            // ID 및 비밀번호 제한
            
            password = await bcrypt.hash(password, 10);
            if (!await models.UserAccount.findOne({ where: { username: username } })) {
                await models.UserAccount.create({ 
                    username, password, role, phone_number, last_name,first_name, email });
                res.send("Done");
            }
            else {
                res.send("username exist")
            }
        }
        catch(err){
            await res.status(500).send({
                message:
                    err.message || "some error occured"
            })
        }
    },

    //로그인
    login: async (req, res, next) => {
        // #swagger.description = "로그인합니다.."
        // #swagger.tags = ["login"]
        // #swagger.summary = "로그인"
        /* #swagger.parameters['association_name'] = {
            in: "query",
            type: "string"
        },
        #swagger.parameters['country_code'] = {
            in: "query",
            type: "string"
        },
        */
        try{
            const { username, password } = req.body;
            const association_name = req.query.association_name
            const country_code = req.query.country_code
            
            user = await models.UserAccount.findOne({ raw: true, where: { username: username } })
            if(user.role == "ASSOCIATION"){
                is_right_association = await models.AssociationInfos.findOne({
                    where: {country_code:country_code, name:association_name, user:user.id},
                    raw: true
                })
                if(!is_right_association){
                    return res.send("wrong association")
                }
            }
            else if((user.role != "ASSOCIATION") && (association_name || country_code)){
                return res.send("id is not association")
            }

            if (user && await bcrypt.compare(password, user.password)) {
                const accessToken = await sign(user);
                const refreshToken = refresh();
                await models.Refresh.create({ userID: user.id, refreshToken: refreshToken })
                res.status(200).send({ // client에게 토큰 모두를 반환합니다.
                    ok: true,
                    data: {
                        accessToken,
                        refreshToken,
                    },
                });
            }
            else {
                res.status(401).send({
                    ok: false,
                    message: 'password is incorrect',
                });
            }
        }
        catch(err){
            await res.status(500).send({
                message:
                    err.message || "some error occured"
            })
        }
    },

    //중복 체크
    Is_Username_Duplicate: async (req, res, next) => {
        // #swagger.description = "username 중복 체크"
        // #swagger.tags = ["중복 체크"]
        // #swagger.summary = "username 중복 체크"
        try{
            const username = req.params.username
            let user_info = await models.UserAccount.findOne({
                raw: true, 
                where: { 
                    username: username
                },
                attributes: ['id']
            })
            
            if(user_info){
                return res.send(true)
            }
            else{
                return res.send(false)
            }
        }
        catch(err){
            await res.status(500).send({
                message:
                    err.message || "some error occured"
            })
        }
    },

    Is_PhoneNumber_Duplicate: async (req, res, next) => {
        // #swagger.description = "phonenumber 중복 체크"
        // #swagger.tags = ["중복 체크"]
        // #swagger.summary = "phonenumber 중복 체크"
        try{
            const phone_number = req.params.phone_number
            let user_info = await models.UserAccount.findOne({
                raw: true, 
                where: { 
                    phone_number: phone_number
                },
                attributes: ['id']
            })
            
            if(user_info){
                return res.send(true)
            }
            else{
                return res.send(false)
            }
        }
        catch(err){
            await res.status(500).send({
                message:
                    err.message || "some error occured"
            })
        }
    },

    //닉네임 찾기
    FindUsernameEmail: async (req, res, next) => {
        // #swagger.description = "이메일을 통해 username을 찾습니다"
        // #swagger.tags = ["아이디 찾기"]
        // #swagger.summary = "username 찾기"
        try{
            const email = req.params.email
            const last_name = req.params.last_name
            const first_name = req.params.first_name
            
            let user_info = await models.UserAccount.findOne({
                raw: true, 
                where: { 
                    email: email,
                    last_name: last_name,
                    first_name: first_name
                },
                attributes: ['username']
            })
            
            if(user_info){
                return res.send(user_info.username)
            }
            else{
                return res.send("Wrong input")
            }
        }
        catch(err){
            await res.status(500).send({
                message:
                    err.message || "some error occured"
            })
        }
    },


    //내 설정 바꾸기
    ChangeMyInfo: async (req, res, next) => {
        // #swagger.description = "내 정보를 수정합니다"
        // #swagger.tags = ["유저"]
        // #swagger.summary = "내 정보 수정"
        try{
            const auth_id = req.id
            const auth_role = req.role
            let { current_password, new_password, email, phone_number } = req.body  
            let user_info = await models.UserAccount.findOne({
                where: {id:auth_id},
                raw:true
            }) 
            if(current_password && new_password){
                if (user_info && await bcrypt.compare(current_password, user_info.password)){
                    new_password = await bcrypt.hash(new_password, 10);
                    await models.UserAccount.update(
                        {
                            password: new_password, email, phone_number
                        },
                        {
                            where: {id: auth_id}
                        }
                    )
                    if(auth_role == "KWANJANG"){
                        await models.KwanjangInfo.update({phone_number, email},
                            {where: {user: auth_id},}
                        )}
                    else if(auth_role == "SABUM"){
                        await models.Sabum.update({phone_number, email},
                            {where: {user: auth_id},}
                        )}
                    else if(auth_role == "FAMILY"){
                        await models.ParentsInfo.update({phone_number, email},
                            {where: {user: auth_id},}
                        )}
                    else if(auth_role == "STUDENT"){
                        await models.StudentInfo.update({phone_number, email},
                            {where: {user: auth_id},}
                        )}
                    res.send("MyInfo is successfully updated")
                }
                else{
                    res.send("wrong password")
                }
            }
            else{
                await models.UserAccount.update(
                    {
                        email, phone_number
                    },
                    {
                        where: {id: auth_id}
                    }
                )
                if(auth_role == "KWANJANG"){
                    await models.KwanjangInfo.update({phone_number, email},
                        {where: {user: auth_id},}
                    )}
                else if(auth_role == "SABUM"){
                    await models.Sabum.update({phone_number, email},
                        {where: {user: auth_id},}
                    )}
                else if(auth_role == "FAMILY"){
                    await models.ParentsInfo.update({phone_number, email},
                        {where: {user: auth_id},}
                    )}
                else if(auth_role == "STUDENT"){
                    await models.StudentInfo.update({phone_number, email},
                        {where: {user: auth_id},}
                    )}
                res.send("MyInfo is successfully updated")
            }
            
        }
        catch(err){
            await res.status(500).send({
                message:
                    err.message || "some error occured"
            })
        }
    },

    //출석
    AttendancesCreate: async (req, res, next) => {
        // #swagger.description = "출석 정보를 생성 합니다"
        // #swagger.tags = ["출석"]
        // #swagger.summary = "출석 생성"
        const { is_attended, date, student } = req.body;
        await models.Attendance.create({ is_attended, date, student })
        res.send("Attendances Successfully created")
    },

    AttendancesRead: async (req, res, next) => {
        // #swagger.description = "출석 정보를 조회 합니다"
        // #swagger.tags = ["출석"]
        // #swagger.summary = "출석 조회"
        id = req.params.id;
        models.Attendances.findAll({
            where: {
                user: id
            },
            include: [
                {
                    model: models.UserAccount,
                    required: true,
                    attributes: ['username'],
                }
            ]
        })
            .then(data => {
                res.send(data);
            })
            .catch(err => {
                res.status(500).send({
                    message:
                        err.message || "some error occured"
                })
            })
    },

    //유저 조회
    UserInfoRead: async (req, res, next) => {
        // #swagger.description = "내 정보를 수정합니다"
        // #swagger.tags = ["안씀"]
        // #swagger.summary = "내 정보 수정"
        date = req.params.date;
        class_id = req.params.class_id;
        models.Attendances.findAll({
            include: [
                {
                    model: models.UserAccount,
                    attributes: ['username'],
                }
            ]
        })
            .then(data => {
                res.send(data);
            })
            .catch(err => {
                res.status(500).send({
                    message:
                        err.message || "some error occured"
                })
            })
    },

    registerFcmToken: async (req, res) => {
        // #swagger.description = "FCM 정보 생성"
        // #swagger.tags = ["FCM"]
        // #swagger.summary = "FCM 정보 생성"
        try{
            let {token, user_id} = req.body;
            await models.UserAccount.update(
                {
                    fcm_token:token
                },
                {
                    where:{
                        id:user_id
                    }
                }
            );
        res.send("Token is successfully registered")
        }catch (err) {
            console.error(err);
        }
    },

    FcmTokenUpdate: async (req, res)=>{
        // #swagger.description = "FCM 정보 수정"
        // #swagger.tags = ["FCM"]
        // #swagger.summary = "FCM 정보 수정"
        try{
            const data = req.body
            await models.UserAccount.update(
                {
                    fcm_token: data.fcm_token
                },
                {
                    where: { id : data.id }
                },
            res.send("fcm token updated")
            )
        }catch(err){
            console.error(err)
        }
    },

    //계정 -> 알림장 
    Alarm_note: async (req, res)=>{
        // #swagger.description = "알림 - 공지"
        // #swagger.tags = ["유저"]
        // #swagger.summary = "알림 - 공지"
        try{
            const dojang_id = req.params.dojang_id
            const auth_id = req.id
            const auth_role = req.role
            const {student_id} =req.body

            let dojang_info = await models.Dojang.findOne({
                where: {id: dojang_id},
                attributes:['name'],
                raw:true
            })

            //접속한 계정이 관장인 경우 (모든 반 해당)
            if(auth_role == 'KWANJANG'){
                //모든 반(sender가 누군지 상관 없음)
                let note_info = await models.Note.findAll({
                    where:{sender_type: "STUDENT", recipient_dojang: dojang_id},
                    attributes: ['id','contents','createdAt','real_sender'],
                    raw:true,
                    order: [['createdAt','desc']]
                })
                //데이터 가공(댓글 createdAt 합치기)
                for(let note_one of note_info){
                    let notereply_info = await models.NoteReply.findAll({
                        where: {note: note_one.id},
                        attributes: ['createdAt'],
                        order: [['createdAt','desc']],
                        raw:true
                    })
                    
                    if(notereply_info[0]){
                        if(note_one.createdAt <= notereply_info[0].createdAt)
                        {
                            note_one.createdAt = notereply_info[0].createdAt
                        }
                    }
                }
                
                //배열 내 객체 정렬
                note_info.sort(function(a,b){
                    return b.createdAt - a.createdAt
                })

                for(let note_one_new of note_info){
                    user_info = new Object
                    let sender_info = await models.UserAccount.findOne({
                        where: {id: note_one_new.real_sender},
                        attributes: ['role'],
                        raw: true
                    })
                    if(sender_info.role == 'FAMILY'){
                        user_info = await models.ParentsInfo.findOne({
                            where: {user: note_one_new.real_sender},
                            attributes: ['id','last_name','first_name'],
                            raw: true
                        })
                    }
                    else if(sender_info.role == 'STUDENT'){
                        user_info = await models.StudentInfo.findOne({
                            where: {user: note_one_new.real_sender},
                            attributes: ['id','last_name','first_name'],
                            raw: true
                        })
                    }
                    note_one_new['last_name'] = user_info.last_name
                    note_one_new['first_name'] = user_info.first_name
                    note_one_new['dojang_name'] = dojang_info.name
                }
                return res.send(note_info)
            }
            if(auth_role == 'SABUM'){
                
                result_arr = []
                //해당 반
                let note_info = await models.Note.findAll({
                    where:{sender_type: "STUDENT", recipient_dojang: dojang_id},
                    attributes: ['id','contents','createdAt','real_sender','sender'],
                    raw:true,
                    order: [['createdAt','desc']]
                })
                //데이터 가공(댓글 createdAt 합치기)
                for(let note_one of note_info){
                    let notereply_info = await models.NoteReply.findAll({
                        where: {note: note_one.id},
                        attributes: ['createdAt'],
                        order: [['createdAt','desc']],
                        raw:true
                    })
                    
                    if(notereply_info[0]){
                        if(note_one.createdAt <= notereply_info[0].createdAt)
                        {
                            note_one.createdAt = notereply_info[0].createdAt
                        }
                    }
                }
                
                //배열 내 객체 정렬
                note_info.sort(function(a,b){
                    return b.createdAt - a.createdAt
                })
                for(let note_one_new of note_info){
                    linked_count = 0
                    result_obj = new Object
                    //각 알림장이 사범과 연관 있는지
                    is_linked = 0
                    //각 알림장의 학생 정보
                    let student_info = await models.StudentInfo.findOne({
                        where: {user: note_one_new.sender},
                        attributes: ['id'],
                        raw:true
                    })
                    //사범 정보
                    let sabum_info = await models.Sabum.findOne({
                        where: {user: auth_id},
                        attributes: ['id'],
                        raw:true
                    })

                    //학생이 다니는 반 리스트
                    let classstudent_info = await models.ClassStudent.findAll({
                        where: {student: student_info.id},
                        attributes: ['class'],
                        raw:true
                    })

                    //학생이 다니는 각 반 리스트 중
                    for(let classstudent_one of classstudent_info){
                        let is_sabumstudent_linked = await models.SabumClass.findOne({
                            where: {sabum: sabum_info.id, class: classstudent_one.class},
                            attributes: ['id'],
                            raw:true
                        })
                        if(is_sabumstudent_linked){
                            is_linked = 1
                        }
                    }

                    if(is_linked && linked_count < 5){
                        user_info = new Object
                        let sender_info = await models.UserAccount.findOne({
                            where: {id: note_one_new.real_sender},
                            attributes: ['role'],
                            raw: true
                        })
                        if(sender_info.role == 'FAMILY'){
                            user_info = await models.ParentsInfo.findOne({
                                where: {user: note_one_new.real_sender},
                                attributes: ['id','last_name','first_name'],
                                raw: true
                            })
                        }
                        else if(sender_info.role == 'STUDENT'){
                            user_info = await models.StudentInfo.findOne({
                                where: {user: note_one_new.real_sender},
                                attributes: ['id','last_name','first_name'],
                                raw: true
                            })
                        }
                        result_obj['id'] = note_one_new.id
                        result_obj['contents'] = note_one_new.contents
                        result_obj['createdAt'] = note_one_new.createdAt
                        result_obj['real_sender'] = note_one_new.real_sender
                        result_obj['last_name'] = user_info.last_name
                        result_obj['first_name'] = user_info.first_name
                        result_obj['dojang_name'] = dojang_info.name
                        result_arr.push(result_obj)

                        linked_count += 1
                    }
                }
                return res.send(result_arr)
            }
            if(auth_role == 'STUDENT'){
                result_arr = []
                //해당 반
                let note_info = await models.Note.findAll({
                    where:{sender_type: "CLASS", recipient: auth_id},
                    attributes: ['id','contents','createdAt','real_sender','sender_dojang'],
                    raw:true,
                    order: [['createdAt','desc']]
                })
                //데이터 가공(댓글 createdAt 합치기)
                for(let note_one of note_info){
                    let notereply_info = await models.NoteReply.findAll({
                        where: {note: note_one.id},
                        attributes: ['createdAt'],
                        order: [['createdAt','desc']],
                        raw:true,
                        limit: 5
                    })
                    
                    if(notereply_info[0]){
                        if(note_one.createdAt <= notereply_info[0].createdAt)
                        {
                            note_one.createdAt = notereply_info[0].createdAt
                        }
                    }
                }
                
                //배열 내 객체 정렬
                note_info.sort(function(a,b){
                    return b.createdAt - a.createdAt
                })
                for(let note_one_new of note_info){
                    result_obj = new Object
                    //각 알림장이 사범과 연관 있는지

                    user_info = new Object
                    let sender_info = await models.UserAccount.findOne({
                        where: {id: note_one_new.real_sender},
                        attributes: ['role'],
                        raw: true
                    })
                    if(sender_info.role == 'KWANJANG'){
                        user_info = await models.KwanjangInfo.findOne({
                            where: {user: note_one_new.real_sender},
                            attributes: ['id','last_name','first_name'],
                            raw: true
                        })
                    }
                    else if(sender_info.role == 'SABUM'){
                        user_info = await models.Sabum.findOne({
                            where: {user: note_one_new.real_sender},
                            attributes: ['id','last_name','first_name'],
                            raw: true
                        })
                    }
                    note_one_new['last_name'] = user_info.last_name
                    note_one_new['first_name'] = user_info.first_name
                    note_one_new['dojang_name'] = dojang_info.name
                }
                return res.send(note_info)
            }
            if(auth_role == 'FAMILY'){
                result_arr = []
                //해당 반
                let note_info = await models.Note.findAll({
                    where:{sender_type: "CLASS", recipient: student_id},
                    attributes: ['id','contents','createdAt','real_sender','sender_dojang'],
                    raw:true,
                    order: [['createdAt','desc']]
                })
                //데이터 가공(댓글 createdAt 합치기)
                for(let note_one of note_info){
                    let notereply_info = await models.NoteReply.findAll({
                        where: {note: note_one.id},
                        attributes: ['createdAt'],
                        order: [['createdAt','desc']],
                        raw:true,
                        limit: 5
                    })
                    
                    if(notereply_info[0]){
                        if(note_one.createdAt <= notereply_info[0].createdAt)
                        {
                            note_one.createdAt = notereply_info[0].createdAt
                        }
                    }
                }
                
                //배열 내 객체 정렬
                note_info.sort(function(a,b){
                    return b.createdAt - a.createdAt
                })
                for(let note_one_new of note_info){
                    result_obj = new Object
                    //각 알림장이 사범과 연관 있는지

                    user_info = new Object
                    let sender_info = await models.UserAccount.findOne({
                        where: {id: note_one_new.real_sender},
                        attributes: ['role'],
                        raw: true
                    })
                    if(sender_info.role == 'KWANJANG'){
                        user_info = await models.KwanjangInfo.findOne({
                            where: {user: note_one_new.real_sender},
                            attributes: ['id','last_name','first_name'],
                            raw: true
                        })
                    }
                    else if(sender_info.role == 'SABUM'){
                        user_info = await models.Sabum.findOne({
                            where: {user: note_one_new.real_sender},
                            attributes: ['id','last_name','first_name'],
                            raw: true
                        })
                    }
                    note_one_new['last_name'] = user_info.last_name
                    note_one_new['first_name'] = user_info.first_name
                    note_one_new['dojang_name'] = dojang_info.name
                }
                return res.send(note_info)
            }
        }
        catch(err){
            await res.status(500).send({
                message:
                    err.message || "some error occured"
            })
        }
    },
}
