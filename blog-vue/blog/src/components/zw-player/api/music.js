import axios from "axios";
//获取歌词
export const getWords = id => {
  return axios.post(`https://service-jmqmt8dk-1252027164.gz.apigw.tencentcs.com/release/lyric?id=${id}`);
};
//获取歌曲详情
export const getMusicInfo = id => {
  return axios.post(`https://service-jmqmt8dk-1252027164.gz.apigw.tencentcs.com/release/song/detail?ids=${id}`);
};
//获取歌曲url
export const getMusicUrl = id => {
  return axios.post(`https://service-jmqmt8dk-1252027164.gz.apigw.tencentcs.com/release/song/url?id=${id}`);
};
//获取热门歌曲
export const getHotMusic = id => {
  return axios.post(`https://service-jmqmt8dk-1252027164.gz.apigw.tencentcs.com/release/playlist/detail?id=${id}`);
};
//获取搜索建议
export const getSearchSuggest = key => {
  return axios.post(`https://service-jmqmt8dk-1252027164.gz.apigw.tencentcs.com/release/search/suggest?keywords=${key}`);
};
//获取歌曲热门评论
export const getHotTalk = id => {
  return axios.post(`https://service-jmqmt8dk-1252027164.gz.apigw.tencentcs.com/release/comment/music?id=${id}&limit=3`);
};
