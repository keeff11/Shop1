import { Ionicons } from '@expo/vector-icons';
import * as ImagePicker from 'expo-image-picker';
import { useRouter } from 'expo-router';
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
import { fetchApi } from '../../../lib/api';

const CATEGORIES = [
  { id: 'ELECTRONICS', label: '💻 전자기기' },
  { id: 'CLOTHING', label: '👕 의류' },
  { id: 'HOME', label: '🏠 가전/생활' },
  { id: 'BOOKS', label: '📚 도서' },
  { id: 'BEAUTY', label: '💄 뷰티/화장품' },
  { id: 'OTHERS', label: '📦 기타' }
];

export default function ItemCreateScreen() {
  const router = useRouter();
  
  const [name, setName] = useState("");
  const [price, setPrice] = useState("");
  const [quantity, setQuantity] = useState("");
  const [category, setCategory] = useState("ELECTRONICS");
  const [description, setDescription] = useState("");
  const [images, setImages] = useState<{ uri: string }[]>([]);
  const [loading, setLoading] = useState(false);

  const handleNumberFormat = (text: string, setter: (val: string) => void) => {
    const rawValue = text.replace(/,/g, "");
    if (rawValue === "") { setter(""); return; }
    if (isNaN(Number(rawValue))) return;
    setter(Number(rawValue).toLocaleString());
  };

  const pickImage = async () => {
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsMultipleSelection: true,
      quality: 0.8,
    });
    if (!result.canceled && result.assets) {
      setImages(prev => [...prev, ...result.assets.map(a => ({ uri: a.uri }))]);
    }
  };

  const removeImage = (index: number) => {
    setImages(prev => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = async () => {
    if (!name || !price || !quantity) return Alert.alert("알림", "필수 정보를 모두 입력해주세요.");

    setLoading(true);
    try {
      const formData = new FormData();
      const itemData = { 
        name, 
        price: Number(price.replace(/,/g, "")), 
        quantity: Number(quantity.replace(/,/g, "")), 
        category, description 
      };
      
      formData.append("request", {
        string: JSON.stringify(itemData), type: 'application/json'
      } as any);

      images.forEach((img) => {
        const filename = img.uri.split('/').pop() || 'item.jpg';
        const match = /\.(\w+)$/.exec(filename);
        const type = match ? `image/${match[1]}` : `image`;
        formData.append("images", { uri: img.uri, name: filename, type } as any);
      });

      const res = await fetchApi<any>("/items", {
        method: "POST", body: formData, headers: { 'Content-Type': 'multipart/form-data' }
      });
      
      Alert.alert("성공", "상품이 성공적으로 등록되었습니다!", [
        { text: "확인", onPress: () => router.push(`/items/${res.data}` as any) }
      ]);
    } catch (error: any) {
      Alert.alert("실패", error.message || "상품 등록에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <View style={styles.header}>
          <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}><Ionicons name="close" size={24} color="#111" /></TouchableOpacity>
          <Text style={styles.headerTitle}>새 상품 등록</Text>
          <View style={{ width: 24 }} />
        </View>

        <ScrollView contentContainerStyle={styles.content}>
          <View style={styles.inputGroup}>
            <Text style={styles.label}>상품명</Text>
            <TextInput style={styles.input} value={name} onChangeText={setName} placeholder="예) 아이폰 15 Pro" />
          </View>

          <View style={styles.row}>
            <View style={[styles.inputGroup, { flex: 1 }]}>
              <Text style={styles.label}>가격 (원)</Text>
              <TextInput style={styles.input} value={price} onChangeText={(t) => handleNumberFormat(t, setPrice)} placeholder="0" keyboardType="numeric" />
            </View>
            <View style={{ width: 12 }} />
            <View style={[styles.inputGroup, { flex: 1 }]}>
              <Text style={styles.label}>재고 수량</Text>
              <TextInput style={styles.input} value={quantity} onChangeText={(t) => handleNumberFormat(t, setQuantity)} placeholder="1" keyboardType="numeric" />
            </View>
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>카테고리</Text>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} style={{ marginHorizontal: -4 }}>
              {CATEGORIES.map(cat => (
                <TouchableOpacity 
                  key={cat.id} 
                  style={[styles.chip, category === cat.id && styles.activeChip]}
                  onPress={() => setCategory(cat.id)}
                >
                  <Text style={[styles.chipText, category === cat.id && styles.activeChipText]}>{cat.label}</Text>
                </TouchableOpacity>
              ))}
            </ScrollView>
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>상품 설명</Text>
            <TextInput 
              style={styles.textArea} value={description} onChangeText={setDescription}
              placeholder="상품의 특징, 상태 등을 자세히 적어주세요." multiline textAlignVertical="top"
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>상품 이미지 ({images.length}개)</Text>
            <ScrollView horizontal showsHorizontalScrollIndicator={false}>
              <TouchableOpacity style={styles.addImageBtn} onPress={pickImage}>
                <Ionicons name="camera" size={24} color="#9ca3af" />
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
            {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.submitBtnText}>상품 등록하기</Text>}
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
  content: { padding: 20, paddingBottom: 60 },
  inputGroup: { marginBottom: 20 },
  label: { fontSize: 14, fontWeight: 'bold', color: '#374151', marginBottom: 8 },
  input: { borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 12, paddingHorizontal: 16, paddingVertical: 14, fontSize: 15, backgroundColor: '#f9fafb' },
  row: { flexDirection: 'row' },
  chip: { paddingHorizontal: 16, paddingVertical: 10, borderRadius: 20, borderWidth: 1, borderColor: '#e5e7eb', backgroundColor: '#fff', marginHorizontal: 4 },
  activeChip: { backgroundColor: '#111', borderColor: '#111' },
  chipText: { fontSize: 14, color: '#4b5563', fontWeight: 'bold' },
  activeChipText: { color: '#fff' },
  textArea: { borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 12, padding: 16, height: 120, fontSize: 15, backgroundColor: '#f9fafb' },
  addImageBtn: { width: 80, height: 80, borderWidth: 1, borderColor: '#d1d5db', borderStyle: 'dashed', borderRadius: 12, justifyContent: 'center', alignItems: 'center', marginRight: 12, backgroundColor: '#f9fafb' },
  imageWrapper: { width: 80, height: 80, marginRight: 12, position: 'relative' },
  previewImage: { width: '100%', height: '100%', borderRadius: 12, borderWidth: 1, borderColor: '#e5e7eb' },
  removeImageBtn: { position: 'absolute', top: -6, right: -6, backgroundColor: '#ef4444', width: 22, height: 22, borderRadius: 11, justifyContent: 'center', alignItems: 'center' },
  bottomBar: { padding: 20, borderTopWidth: 1, borderTopColor: '#f3f4f6', backgroundColor: '#fff' },
  submitBtn: { backgroundColor: '#111', paddingVertical: 16, borderRadius: 12, alignItems: 'center' },
  disabledBtn: { backgroundColor: '#d1d5db' },
  submitBtnText: { color: '#fff', fontSize: 16, fontWeight: 'bold' }
});