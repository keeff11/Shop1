import { Ionicons } from '@expo/vector-icons';
import * as ImagePicker from 'expo-image-picker';
import { useLocalSearchParams, useRouter } from 'expo-router';
import React, { useState } from 'react';
import {
    ActivityIndicator,
    Alert,
    Image,
    KeyboardAvoidingView, Platform,
    SafeAreaView,
    ScrollView,
    StyleSheet,
    Text, TextInput, TouchableOpacity,
    View
} from 'react-native';
import { fetchApi } from '../../lib/api';

export default function ReviewCreateScreen() {
  const router = useRouter();
  const { orderItemId, itemName } = useLocalSearchParams<{ orderItemId: string, itemName: string }>();

  const [rating, setRating] = useState(5);
  const [content, setContent] = useState("");
  const [images, setImages] = useState<{ uri: string }[]>([]);
  const [loading, setLoading] = useState(false);

  const pickImage = async () => {
    if (images.length >= 5) return Alert.alert("알림", "이미지는 최대 5장까지 첨부 가능합니다.");

    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsMultipleSelection: true,
      selectionLimit: 5 - images.length,
      quality: 0.8,
    });

    if (!result.canceled && result.assets) {
      const newImages = result.assets.map(asset => ({ uri: asset.uri }));
      setImages(prev => [...prev, ...newImages].slice(0, 5));
    }
  };

  const removeImage = (index: number) => {
    setImages(prev => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = async () => {
    if (content.length < 10) return Alert.alert("알림", "리뷰 내용을 10자 이상 작성해주세요.");

    setLoading(true);
    try {
      const formData = new FormData();
      const reviewData = { rating, content };
      
      formData.append("review", {
        string: JSON.stringify(reviewData), type: 'application/json'
      } as any);

      images.forEach((img) => {
        const filename = img.uri.split('/').pop() || 'image.jpg';
        const match = /\.(\w+)$/.exec(filename);
        const type = match ? `image/${match[1]}` : `image`;
        formData.append("images", { uri: img.uri, name: filename, type } as any);
      });

      await fetchApi(`/reviews?orderItemId=${orderItemId}`, {
        method: "POST",
        body: formData,
        headers: { 'Content-Type': 'multipart/form-data' }
      });

      Alert.alert("성공", "리뷰가 소중하게 등록되었습니다! ✨", [
        { text: "확인", onPress: () => router.back() }
      ]);
    } catch (err: any) {
      Alert.alert("등록 실패", err.message || "서버 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <View style={styles.header}>
          <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}><Ionicons name="close" size={24} color="#111" /></TouchableOpacity>
          <Text style={styles.headerTitle}>리뷰 작성</Text>
          <View style={{ width: 24 }} />
        </View>

        <ScrollView contentContainerStyle={styles.content}>
          <Text style={styles.itemName}>{itemName}</Text>

          <View style={styles.ratingContainer}>
            <Text style={styles.label}>상품은 어떠셨나요?</Text>
            <View style={styles.starsRow}>
              {[1, 2, 3, 4, 5].map(num => (
                <TouchableOpacity key={num} onPress={() => setRating(num)}>
                  <Ionicons name="star" size={40} color={num <= rating ? "#fbbf24" : "#e5e7eb"} />
                </TouchableOpacity>
              ))}
            </View>
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>상세 리뷰</Text>
            <TextInput 
              style={styles.textArea} value={content} onChangeText={setContent}
              placeholder="최소 10자 이상 작성해주세요." multiline textAlignVertical="top"
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>사진 첨부 ({images.length}/5)</Text>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.imageScroll}>
              <TouchableOpacity style={styles.addImageBtn} onPress={pickImage}>
                <Ionicons name="camera" size={24} color="#9ca3af" />
                <Text style={styles.addImageText}>추가</Text>
              </TouchableOpacity>
              {images.map((img, idx) => (
                <View key={idx} style={styles.imageWrapper}>
                  <Image source={{ uri: img.uri }} style={styles.previewImage} />
                  <TouchableOpacity style={styles.removeImageBtn} onPress={() => removeImage(idx)}>
                    <Ionicons name="close" size={12} color="#fff" />
                  </TouchableOpacity>
                </View>
              ))}
            </ScrollView>
          </View>
        </ScrollView>

        <View style={styles.bottomBar}>
          <TouchableOpacity style={[styles.submitBtn, loading && styles.disabledBtn]} onPress={handleSubmit} disabled={loading}>
            {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.submitBtnText}>리뷰 등록하기</Text>}
          </TouchableOpacity>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: '#fff' },
  header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', padding: 16, borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  backBtn: { padding: 4 },
  headerTitle: { fontSize: 18, fontWeight: 'bold', color: '#111' },
  content: { padding: 24, paddingBottom: 60 },
  itemName: { fontSize: 16, color: '#4b5563', textAlign: 'center', marginBottom: 30, fontWeight: '500' },
  ratingContainer: { alignItems: 'center', marginBottom: 30 },
  label: { fontSize: 14, fontWeight: 'bold', color: '#374151', marginBottom: 12 },
  starsRow: { flexDirection: 'row', gap: 8 },
  inputGroup: { marginBottom: 24 },
  textArea: { borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 12, padding: 16, height: 160, fontSize: 15, backgroundColor: '#f9fafb' },
  imageScroll: { flexDirection: 'row', paddingVertical: 4 },
  addImageBtn: { width: 80, height: 80, borderWidth: 1, borderColor: '#d1d5db', borderStyle: 'dashed', borderRadius: 12, justifyContent: 'center', alignItems: 'center', marginRight: 12, backgroundColor: '#f9fafb' },
  addImageText: { fontSize: 12, color: '#9ca3af', marginTop: 4 },
  imageWrapper: { width: 80, height: 80, marginRight: 12, position: 'relative' },
  previewImage: { width: '100%', height: '100%', borderRadius: 12, borderWidth: 1, borderColor: '#f3f4f6' },
  removeImageBtn: { position: 'absolute', top: -6, right: -6, backgroundColor: '#ef4444', width: 20, height: 20, borderRadius: 10, justifyContent: 'center', alignItems: 'center' },
  bottomBar: { padding: 20, borderTopWidth: 1, borderTopColor: '#f3f4f6', backgroundColor: '#fff' },
  submitBtn: { backgroundColor: '#111', paddingVertical: 16, borderRadius: 12, alignItems: 'center' },
  disabledBtn: { backgroundColor: '#d1d5db' },
  submitBtnText: { color: '#fff', fontSize: 16, fontWeight: 'bold' }
});