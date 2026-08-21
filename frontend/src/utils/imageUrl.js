export const getImageUrl = (imageUrl) => {
    if (!imageUrl) {
        return ''
    }

    // 原本就是完整網址，不處理
    if (
        imageUrl.startsWith('http://') ||
        imageUrl.startsWith('https://')
    ) {
        return imageUrl
    }

    // 後端上傳的本機圖片
    return `http://localhost:8080${imageUrl}`
}