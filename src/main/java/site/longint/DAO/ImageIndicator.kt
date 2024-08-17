package site.longint.DAO

import kotlinx.serialization.Serializable
import net.mamoe.mirai.Bot
import net.mamoe.mirai.message.data.Image


@Serializable
class ImageIndicator(var imgid: String = "",
                     var nativeURI: String = "",
                     var type: String = "",
                     var width: Int = 0,
                     var height: Int = 0){
    constructor(img: Image):this(){
        imgid = img.imageId
        type = img.imageType.toString()
        width = img.width
        height = img.height
    }
    constructor(img: Image, path: String):this(){
        imgid = img.imageId
        type = img.imageType.toString()
        width = img.width
        height = img.height
        nativeURI = path
    }
}