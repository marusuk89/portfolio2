const Sequelize = require('sequelize');
const Op = Sequelize.Op;
const { sequelize } = require('../models')
const models = require("../models");

const crypto = require("crypto");
const generateFileName = (bytes = 8) =>
    crypto.randomBytes(bytes).toString("hex");
const { uploadFile} = require("../util/s3.js");

module.exports = {
    //class
    ClassCreate: async (req,res,next) => {
        /* #swagger.description = "반을 만듭니다. kwanjang, dojang column INTEGER necessary" <br />
        mon_time 등 시간 입력시 ex) 12:00~13:00 처럼 "시작시간~끝나는시간"의 포맷 필요
        */
        // #swagger.tags = ["반"]
        // #swagger.summary = "반 생성"
        try{
            const {title, room, kwanjang, dojang, mon_time, tue_time, wed_time, thu_time, fri_time, sat_time, sun_time} = req.body;
            await models.Class.create({ title, room, kwanjang, dojang, mon_time, tue_time, wed_time, thu_time, fri_time, sat_time, sun_time})
            res.send("Class Successfully created")
        }
        catch(err){
            await res.status(500).send({
                message:
                    err.message || "some error occured"
            })
        }
    },
    ClassRead: async (req, res, next) => {
        // #swagger.description = "반을 조회합니다. id가 0이면 모두 검색, 0이 아닐 시 해당 id만 검색합니다."
        // #swagger.tags = ["반"]
        // #swagger.summary = "반 조회"
        /* #swagger.parameters['page'] = {
            in: "query",
            description: "페이지",
            type: "integer"
        }
        */
        class_id = req.params.class_id;
        dojang_id = req.params.dojang_id;
        let pageNum = req.query.page; // 요청 페이지 넘버
        let offset = 0;
        if(pageNum > 1){
            offset = 7 * (pageNum - 1);
        }
        try{
            if (class_id != 0) 
            { // id 0이 아닐 때 하나 찾기
                const data = await models.Class.findAll({
                    offset: offset,
                    where: {id:class_id},
                    limit: 7,
                    include: [
                        {
                            model: models.Dojang,
                            where: {id:dojang_id},
                            attributes: ['id','name','phone_number','address_name','address_detail','BR','BR_number','logo_img']
                        },
                        {
                            model: models.Sabum,
                            attributes: ['first_name','last_name','photo_url'],
                            through: {
                                attributes: ['class','sabum']
                            }
                        },
                        {
                            model: models.KwanjangInfo,
                            attributes: ['first_name','last_name','photo_url'],
                            through: {
                                attributes: ['class','kwanjang']
                            }
                        },
                        {
                            model: models.StudentInfo,
                            attributes: ['first_name','last_name','ssn','photo_url','level'],
                            through: {
                                attributes: ['class','student']
                            }
                        }
                    ],
                })
                res.send(data)
            }
            else if (class_id == 0) { // id == 0 이면 모두 찾기
                const data = await models.Class.findAll({
                    offset: offset,
                    limit: 7,
                    include: [
                        {
                            model: models.Dojang,
                            where: {id:dojang_id},
                            attributes: ['id','name','phone_number','address_name','address_detail','BR','BR_number','logo_img']
                        },
                        {
                            model: models.Sabum,
                            attributes: ['first_name','last_name','photo_url'],
                            through: {
                                attributes: ['class','sabum']
                            }
                        },
                        {
                            model: models.KwanjangInfo,
                            attributes: ['first_name','last_name','photo_url'],
                            through: {
                                attributes: ['class','kwanjang']
                            }
                        },
                        {
                            model: models.StudentInfo,
                            attributes: ['first_name','last_name','ssn','photo_url','level'],
                            through: {
                                attributes: ['class','student']
                            }
                        }
                    ],
                })
                res.send(data)
            }
        }
        catch(err) {
            res.status(500).send({
                message:
                    err.message || "some error occured"
            })
        }
    },

    ClassUpdate: async (req,res) => {
        /* #swagger.description = "반을 수정합니다."<br />
        mon_time 등 시간 입력시 ex) 12:00~13:00 처럼 "시작시간~끝나는시간"의 포맷 필요
        */
        // #swagger.tags = ["반"]
        // #swagger.summary = "반 수정"
        const data = req.body;
        const class_id = req.params.class_id;
        await models.Class.update(
        { 
            time: data.time,
            title: data.title,
            room: data.room,
            kwanjang: data.kwanjang,
            dojang: data.dojang,
            mon_time: data.mon_time,
            tue_time: data.tue_time,
            wed_time: data.wed_time,
            thu_time: data.thu_time,
            fri_time: data.fri_time,
            sat_time: data.sat_time,
            sun_time: data.sun_time
        },
        {
            where : { id: class_id }
        } 
        ).then(() =>{
            res.send("Class successfully updated")
        }).catch(err => {
            console.error(err);
        })
    },

    ClassDelete: async (req,res) => {
        // #swagger.description = "반을 지웁니다."
        // #swagger.tags = ["반"]
        // #swagger.summary = "반 삭제"
        try{
            const class_id = req.params.class_id;
            await models.Class.destroy(
                {
                    where : { id: class_id }
                }
            ) 
            res.send("Class successfully deleted")
        }   
        catch(err){
            res.status(500).send({
                message:
                    err.message || "some error occured"
            })
        }
    },

    NoticeCreate: async (req,res) => {
        // #swagger.description = "날짜, 반 정보를 받아 공지를 생성합니다."
        // #swagger.tags = ["공지"]
        // #swagger.summary = "공지 생성"
        try{
            let FILE_img = req.files['img'];
            let FILE_vid = req.files['vid'];
            let FILE_doc = req.files['doc'];

            let photo_url = []
            let video_url = []
            let doc_url = []
            let dojang_id = req.params.dojang_id
            let { class_arr, title, contents} = req.body;
            if(await models.Notice.findOne({
                where: {
                    title: title,
                    contents: contents
                },
                raw: true
            })){return res.send("data exist!!!!")}
            //img
            if(FILE_img){
                for ( var i = 0; i < FILE_img.length; i++){
                    let imageName = generateFileName();
                    if(await models.UrlGroup.findOne({
                        where: {urls:"notice/img/"+imageName}
                    })){imageName = generateFileName();}
                    imageName = "notice/img/"+imageName
                    await models.UrlGroup.create({urls:imageName}) //url group에 늘리기
                    await uploadFile(FILE_img[i].buffer, imageName, FILE_img[i].mimetype)
                    photo_url.push(imageName)
                }
            }
            //vid
            if(FILE_vid){
                for ( var i = 0; i < FILE_vid.length; i++){
                    let imageName = generateFileName();
                    if(await models.UrlGroup.findOne({
                        where: {urls:"notice/vid/"+imageName}
                    })){imageName = generateFileName();}
                    imageName = "notice/vid/"+imageName
                    await models.UrlGroup.create({urls:imageName}) //url group에 늘리기
                    await uploadFile(FILE_vid[i].buffer, imageName, FILE_vid[i].mimetype)
                    video_url.push(imageName)
                }
            }
            //doc
            if(FILE_doc){
                for ( var i = 0; i < FILE_doc.length; i++){
                    let imageName = generateFileName();
                    if(await models.UrlGroup.findOne({
                        where: {urls:"notice/doc/"+imageName}
                    })){imageName = generateFileName();}
                    imageName = "notice/doc/"+imageName
                    await models.UrlGroup.create({urls:imageName}) //url group에 늘리기
                    await uploadFile(FILE_doc[i].buffer, imageName, FILE_doc[i].mimetype)
                    doc_url.push(imageName)
                }
            }
            photo_url = JSON.stringify(photo_url)
            video_url = JSON.stringify(video_url)
            doc_url = JSON.stringify(doc_url)
            if(photo_url.length == 2){
                photo_url = null
            }
            if(video_url.length == 2){
                video_url = null
            }
            if(doc_url.length == 2){
                doc_url = null
            }
            
            await models.Notice.create({ title, contents, img_url: photo_url, vid_url: video_url, doc_url: doc_url})
            
            const notice_id = await models.Notice.findOne({
                where: {
                    title: title,
                    contents: contents
                },
                raw: true
            })

            class_arr= JSON.parse(class_arr)
            arr_len = class_arr.length;

            for(i=0;i<arr_len;i++){
                if(await models.ClassNotice.findOne({where:{
                    class:class_arr[i],notice:notice_id.id,dojang:dojang_id}}
                    )){
                    return res.send("data exist")
                }
                await models.ClassNotice.create({class:class_arr[i],notice:notice_id.id,dojang:dojang_id})
            }
            
            res.send("Notice Successfully created")
        }   
        catch(err){
            res.status(500).send({
                message:
                    err.message || "some error occured"
            })
        }
    },

    NoticeReadApp: async (req, res, next) => {
        /* #swagger.description = "모든 공지를 조회합니다 <br />
        모바일 전용 api 입니다 <br />
        전체 공지와 일반공지로 나누어 조회 합니다 <br />
        "
        */
        // #swagger.tags = ["공지"]
        // #swagger.summary = "공지 조회"
        
        dojang_id = req.params.dojang_id;
        class_id = req.params.class_id;
        year = req.params.year;
        month = req.params.month;
        try{
            if (!await models.Dojang.findOne({
                where : {id : dojang_id}
            })){return res.send("dojang_id not exist")}
            
            // notice_info = await 
            // 도장에 해당되는 모든 notice 의 수 구하기,
            // 도장에 있는 모든 반 구하기
            // 전체 공지 = 해당 notice 일때 모든 반 수와 일치하면
            // 아니라면 일부 공지 
            let result_obj = new Object
            let notice_arr = []
            let notice_data_arr = []

            let dojang_info = await models.Dojang.findOne({
                where: {id: dojang_id},
                attributes: ['name','logo_img'],
                raw: true
            })
            //전체 공지
            if(class_id == 0){
                let notice_info = await models.ClassNotice.findAll({
                    where:{
                        dojang: dojang_id,
                        createdAt: sequelize.where(sequelize.fn('YEAR', sequelize.col('createdAt')), year),
                        [Op.and]: sequelize.where(sequelize.fn('MONTH', sequelize.col('createdAt')), month)
                    },
                    raw:true,
                    attributes: ['class','notice','createdAt'],
                    group: "ClassNotice.notice"
                })
                for(let el of notice_info){
                    el.createdAt = JSON.stringify(el.createdAt).slice(1,-1)
                    el.createdAt = el.createdAt.split('T')[0]

                    if(el.class == null){
                        notice_arr.push(el)
                    }
                    //해당 공지의 반의 수 정보
                    //총 공지의 수 정보
                    
                }
                for(let notice of notice_arr){
                                        
                    let notice_info=await models.Notice.findOne({
                        where: {id:notice.notice},
                        attributes: ['id','title','contents','createdAt'],
                        raw:true
                    })
                    notice_info.createdAt = JSON.stringify(notice_info.createdAt).slice(1,-1)
                    notice_info.createdAt = notice_info.createdAt.split('T')[0]
                    
                    //댓글
                    let reply_info = await models.NoticeReply.findAndCountAll({
                        where: {notice: notice.notice},
                        attributes: ['user'],
                        raw:true
                    })
                    notice_info["reply_count"] = reply_info.count
                    notice_data_arr.push(notice_info)
                }
                result_obj["dojang_info"] = dojang_info
                result_obj["entire"] = notice_data_arr
            }
            //반 공지
            else if(class_id != 0){
                let class_info = await models.Class.findOne({
                    where: {id: class_id},
                    attributes: ['title']
                })
                let entire_info = await models.ClassNotice.findAll({
                    where:{
                        class: null,
                        dojang: dojang_id,
                        createdAt: sequelize.where(sequelize.fn('YEAR', sequelize.col('createdAt')), year),
                        [Op.and]: sequelize.where(sequelize.fn('MONTH', sequelize.col('createdAt')), month)
                    },
                    raw:true,
                    attributes: ['class','notice','createdAt'],
                    group: "ClassNotice.notice"
                })
                let notice_info = await models.ClassNotice.findAll({
                    where:{
                        class: class_id,
                        dojang: dojang_id,
                        createdAt: sequelize.where(sequelize.fn('YEAR', sequelize.col('createdAt')), year),
                        [Op.and]: sequelize.where(sequelize.fn('MONTH', sequelize.col('createdAt')), month)
                    },
                    raw:true,
                    attributes: ['class','notice','createdAt'],
                    group: "ClassNotice.notice"
                })
                for(let el of notice_info){
                    notice_arr.push(el)
                    //해당 공지의 반의 수 정보
                    //총 공지의 수 정보
                    
                }
                for(let notice of notice_arr){
                    let notice_info=await models.Notice.findOne({
                        where: {id:notice.notice},
                        attributes: ['id','title','contents','createdAt'],
                        raw:true
                    })
                    notice_info.createdAt = JSON.stringify(notice_info.createdAt).slice(1,-1)
                    notice_info.createdAt = notice_info.createdAt.split('T')[0]
                    
                    //댓글
                    let reply_info = await models.NoticeReply.findAndCountAll({
                        where: {notice: notice.notice},
                        attributes: ['user'],
                        raw:true
                    })
                    notice_info["reply_count"] = reply_info.count
                    notice_data_arr.push(notice_info)
                }
                result_obj["dojang_name"] = dojang_info.name
                result_obj["dojang_logo"] = dojang_info.logo_img
                result_obj["class_title"] = class_info.title
                result_obj["entire_info"] = entire_info
                result_obj["notice_info"] = notice_data_arr
            }
            res.send(result_obj)
            }
        catch(err){
            await res.status(500).send({
                message:
                    err.message || "some error occured"
            })
        }
    },

    NoticeClassRead: async (req, res, next) => {
        /* #swagger.description = "최신 공지 5개를 조회합니다 <br />
        "
        */
        // #swagger.tags = ["공지"]
        // #swagger.summary = "공지 최신 조회"
        
        try{
            const auth_id = req.id
            const auth_role = req.role
            const dojang_id = req.params.dojang_id

            let class_arr = []

            if(auth_role == 'KWANJANG'){
                const class_info = await models.Class.findAll({
                    where: {dojang: dojang_id},
                    attributes: ['id','title'],
                    raw:true
                })
                return res.send(class_info)
            }
            else if(auth_role == 'SABUM'){
                const sabum_info = await models.Sabum.findOne({
                    where: {user: auth_id},
                    attributes: ['id'],
                    raw:true
                })
                const sabumclass_info = await models.SabumClass.findAll({
                    where: {sabum: sabum_info.id},
                    attributes: ['class'],
                    raw:true
                })
                for(let sabumclass_one of sabumclass_info){
                    class_info = await models.Class.findOne({
                        where: {id: sabumclass_one.class},
                        attributes: ['id','title'],
                        raw:true
                    })
                    class_arr.push(class_info)
                }
                return res.send(class_arr)
            }
        }
        catch(err){
            await res.status(500).send({
                message:
                    err.message || "some error occured"
            })
        }
    },

    NoticeReadAll: async (req, res, next) => {
        /* #swagger.description = "모든 공지를 조회합니다 <br />
        전체공지는 class의 값이 +entire+ -> 따옴표를 쓸수가 없어서 +로 대신합니다 <br />
        일부 공지는 class의 값이 [1,3,5] <br />
        "
        */
        // #swagger.tags = ["공지"]
        // #swagger.summary = "공지 조회"
        
        dojang_id = req.params.dojang_id;

        try{
            if (!await models.Dojang.findOne({
                where : {id : dojang_id}
            })){return res.send("dojang_id not exist")}
            let query = `
                        SELECT CN.notice, N.title, N.createdAt FROM Notices AS N
                        INNER JOIN ClassNotices AS CN
                        ON CN.notice=N.id
                        WHERE CN.dojang=${dojang_id}
                        GROUP BY CN.notice
                        `
            const notice_info = await sequelize.query(query, 
                {
                    type: Sequelize.QueryTypes.SELECT, 
                    raw: true   
                });
            // 도장에 해당되는 모든 notice 의 수 구하기,
            // 도장에 있는 모든 반 구하기
            // 전체 공지 = 해당 notice 일때 모든 반 수와 일치하면
            // 아니라면 일부 공지 
            for(let el of notice_info){
                el.createdAt = JSON.stringify(el.createdAt).slice(1,-1)
                el.createdAt = el.createdAt.split('T')[0]
                CN = await models.ClassNotice.findAndCountAll({
                    where: {notice: el["notice"]},
                    raw:true,
                    attributes: ['class','dojang']
                })
                if(CN.rows[0]['class'] == null){
                    el["class"] = "entire"
                }else{
                    let temp = []
                    CN.rows.forEach(el=>{
                        temp.push(el.class)
                    })
                    el["class"] = temp
                }
                
            }
            
            res.send(notice_info)
            }
        catch(err){
            await res.status(500).send({
                message:
                    err.message || "some error occured"
            })
        }
    },

    NoticeDelete: async (req, res, next) => {
        // #swagger.description = "공지ID를 받아 공지을 삭제합니다"
        // #swagger.tags = ["공지"]
        // #swagger.summary = "공지 삭제"
        const notice_id = req.params.notice_id
        try{
            await models.ClassNotice.destroy({ 
                where: {notice: notice_id}
            })
            await models.Notice.destroy({
                where: {id: notice_id}
            })
            res.send("Notice Successfully deleted")
        }   
        catch(err){
            res.status(500).send({
                message:
                    err.message || "some error occured"
            })
        }
    },

    NoticeReplyCreate: async (req, res, next) => {
        /* #swagger.description = "공지ID를 받아 댓글을 만듭니다.<br />
        작성자가 부모인 경우 student_useraccount_id에 학생의 useraccount ID를 입력해주셔야 합니다(댓글 작성자 관계명시를 위함)
        "
        */
        // #swagger.tags = ["공지"]
        // #swagger.summary = "공지 댓글 생성"
        
        try{
            const notice_id = req.params.notice_id
            const { contents, student_useraccount_id } = req.body
            await models.NoticeReply.create({
                notice: notice_id, user: req.id, contents, student_useraccount_id
            })
            res.send("NoticeReply Successfully created")
        }   
        catch(err){
            res.status(500).send({
                message:
                    err.message || "some error occured"
            })
        }
    },
}
