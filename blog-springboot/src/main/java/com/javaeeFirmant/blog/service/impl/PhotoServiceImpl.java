package com.javaeeFirmant.blog.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.javaeeFirmant.blog.constant.CommonConst;
import com.javaeeFirmant.blog.dao.PhotoDao;
import com.javaeeFirmant.blog.dto.PhotoBackDTO;
import com.javaeeFirmant.blog.dto.PhotoDTO;
import com.javaeeFirmant.blog.entity.Photo;
import com.javaeeFirmant.blog.entity.PhotoAlbum;
import com.javaeeFirmant.blog.exception.BizException;
import com.javaeeFirmant.blog.service.PhotoAlbumService;
import com.javaeeFirmant.blog.service.PhotoService;
import com.javaeeFirmant.blog.util.BeanCopyUtils;
import com.javaeeFirmant.blog.util.PageUtils;
import com.javaeeFirmant.blog.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.javaeeFirmant.blog.enums.PhotoAlbumStatusEnum.PUBLIC;

/**
 * 照片服务
 */
@Service
public class PhotoServiceImpl extends ServiceImpl<PhotoDao, Photo> implements PhotoService {
    @Autowired
    private PhotoDao photoDao;
    @Autowired
    private PhotoAlbumService photoAlbumService;

    @Override
    public PageResult<PhotoBackDTO> listPhotos(ConditionVO condition) {
        // 查询照片列表
        Page<Photo> page = new Page<>(PageUtils.getCurrent(), PageUtils.getSize());
        Page<Photo> photoPage = photoDao.selectPage(page, new LambdaQueryWrapper<Photo>()
                .eq(Objects.nonNull(condition.getAlbumId()), Photo::getAlbumId, condition.getAlbumId())
                .eq(Photo::getIsDelete, condition.getIsDelete())
                .orderByDesc(Photo::getId)
                .orderByDesc(Photo::getUpdateTime));
        List<PhotoBackDTO> photoList = BeanCopyUtils.copyList(photoPage.getRecords(), PhotoBackDTO.class);
        return new PageResult<>(photoList, (int) photoPage.getTotal());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updatePhoto(PhotoInfoVO photoInfoVO) {
        Photo photo = BeanCopyUtils.copyObject(photoInfoVO, Photo.class);
        photoDao.updateById(photo);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void savePhotos(PhotoVO photoVO) {
        List<Photo> photoList = photoVO.getPhotoUrlList().stream().map(item -> Photo.builder()
                        .albumId(photoVO.getAlbumId())
                        .photoName(IdWorker.getIdStr())
                        .photoSrc(item)
                        .build())
                .collect(Collectors.toList());
        this.saveBatch(photoList);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updatePhotosAlbum(PhotoVO photoVO) {
        List<Photo> photoList = photoVO.getPhotoIdList().stream().map(item -> Photo.builder()
                        .id(item)
                        .albumId(photoVO.getAlbumId())
                        .build())
                .collect(Collectors.toList());
        this.updateBatchById(photoList);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updatePhotoDelete(DeleteVO deleteVO) {
        // 更新照片状态
        List<Photo> photoList = deleteVO.getIdList().stream().map(item -> Photo.builder()
                        .id(item)
                        .isDelete(deleteVO.getIsDelete())
                        .build())
                .collect(Collectors.toList());
        this.updateBatchById(photoList);
        // 若恢复照片所在的相册已删除，恢复相册
        if (deleteVO.getIsDelete().equals(CommonConst.FALSE)) {
            List<PhotoAlbum> photoAlbumList = photoDao.selectList(new LambdaQueryWrapper<Photo>()
                            .select(Photo::getAlbumId)
                            .in(Photo::getId, deleteVO.getIdList())
                            .groupBy(Photo::getAlbumId))
                    .stream()
                    .map(item -> PhotoAlbum.builder()
                            .id(item.getAlbumId())
                            .isDelete(CommonConst.FALSE)
                            .build())
                    .collect(Collectors.toList());
            photoAlbumService.updateBatchById(photoAlbumList);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deletePhotos(List<Integer> photoIdList) {
        photoDao.deleteBatchIds(photoIdList);
    }

    @Override
    public PhotoDTO listPhotosByAlbumId(Integer albumId) {
        // 查询相册信息
        PhotoAlbum photoAlbum = photoAlbumService.getOne(new LambdaQueryWrapper<PhotoAlbum>()
                .eq(PhotoAlbum::getId, albumId)
                .eq(PhotoAlbum::getIsDelete, CommonConst.FALSE)
                .eq(PhotoAlbum::getStatus, PUBLIC.getStatus()));
        if (Objects.isNull(photoAlbum)) {
            throw new BizException("相册不存在");
        }
        // 查询照片列表
        Page<Photo> page = new Page<>(PageUtils.getCurrent(), PageUtils.getSize());
        List<String> photoList = photoDao.selectPage(page, new LambdaQueryWrapper<Photo>()
                        .select(Photo::getPhotoSrc)
                        .eq(Photo::getAlbumId, albumId)
                        .eq(Photo::getIsDelete, CommonConst.FALSE)
                        .orderByDesc(Photo::getId))
                .getRecords()
                .stream()
                .map(Photo::getPhotoSrc)
                .collect(Collectors.toList());
        return PhotoDTO.builder()
                .photoAlbumCover(photoAlbum.getAlbumCover())
                .photoAlbumName(photoAlbum.getAlbumName())
                .photoList(photoList)
                .build();
    }

}




