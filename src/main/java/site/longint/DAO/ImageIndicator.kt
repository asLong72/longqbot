package site.longint.DAO

import kotlinx.serialization.Serializable
import net.mamoe.mirai.Bot
import net.mamoe.mirai.message.data.Image


@Serializable
class ImageIndicator(var imgid: String = "",
                     var nativeURI: String = "",
                     var width: Int = 0,
                     var height: Int = 0){
    constructor(img: Image):this(){
        imgid = img.imageId
        width = img.width
        height = img.height
    }
}